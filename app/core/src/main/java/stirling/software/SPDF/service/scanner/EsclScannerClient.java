package stirling.software.SPDF.service.scanner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

import stirling.software.SPDF.service.scanner.ScannerHostGuard.Target;

/**
 * Talks to network scanners over eSCL (AirScan), the HTTP protocol most current Canon, HP, Epson,
 * Brother and Xerox scanners speak. Every method takes a {@link Target} already checked by {@link
 * ScannerHostGuard}.
 */
@Slf4j
@Service
public class EsclScannerClient {

    private static final String ESCL_ROOT = "/eSCL";
    private static final String JOBS_COLLECTION = ESCL_ROOT + "/ScanJobs";
    private static final String JOBS_PATH = JOBS_COLLECTION + "/";
    private static final int MAX_PAGES = 200;
    private static final int BUSY_RETRIES = 120;
    private static final long BUSY_WAIT_MILLIS = 1000;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    /** Largest scannable area of one input source, in 300ths of an inch. */
    public record SourceLimits(String source, int maxWidth, int maxHeight) {}

    public record Capabilities(
            List<SourceLimits> sources, List<Integer> resolutions, List<String> colorModes) {}

    /** One scanned page as a JPEG, with the resolution it was scanned at. */
    public record ScannedPage(byte[] jpeg, int resolution) {}

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

    public Capabilities capabilities(Target target) throws IOException, InterruptedException {
        HttpResponse<byte[]> response =
                send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                target.baseUrl()
                                                        + ESCL_ROOT
                                                        + "/ScannerCapabilities"))
                                .timeout(REQUEST_TIMEOUT)
                                .GET()
                                .build());
        if (response.statusCode() != 200) {
            throw new IOException("Scanner answered capabilities with " + response.statusCode());
        }
        return parseCapabilities(response.body());
    }

    /**
     * Scans every page the source yields. {@code source} is {@code Platen} or {@code Feeder};
     * {@code colorMode} is an eSCL color mode such as {@code RGB24}.
     */
    public List<ScannedPage> scan(
            Target target, String source, int resolution, String colorMode, SourceLimits limits)
            throws IOException, InterruptedException {
        HttpResponse<byte[]> created =
                send(
                        HttpRequest.newBuilder(URI.create(target.baseUrl() + JOBS_COLLECTION))
                                .timeout(REQUEST_TIMEOUT)
                                .header("Content-Type", "text/xml")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                settingsXml(source, resolution, colorMode, limits),
                                                StandardCharsets.UTF_8))
                                .build());
        if (created.statusCode() != 201) {
            throw new IOException("Scanner refused the scan job with " + created.statusCode());
        }

        // Only the path is used from Location: the host it names was never checked.
        String location =
                created.headers()
                        .firstValue("Location")
                        .orElseThrow(() -> new IOException("Scanner did not return a job address"));
        String jobPath = URI.create(location).getPath();
        if (jobPath == null || !jobPath.startsWith(JOBS_PATH) || jobPath.contains("..")) {
            throw new IOException("Scanner returned an unexpected job address");
        }

        String jobBase =
                jobPath.endsWith("/") ? jobPath.substring(0, jobPath.length() - 1) : jobPath;
        List<ScannedPage> pages = new ArrayList<>();
        int busy = 0;
        while (pages.size() < MAX_PAGES) {
            HttpResponse<byte[]> next =
                    send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    target.baseUrl() + jobBase + "/NextDocument"))
                                    .timeout(REQUEST_TIMEOUT)
                                    .GET()
                                    .build());
            int status = next.statusCode();
            if (status == 200) {
                pages.add(new ScannedPage(next.body(), resolution));
            } else if (status == 404) {
                break;
            } else if (status == 503 && busy++ < BUSY_RETRIES) {
                Thread.sleep(BUSY_WAIT_MILLIS);
            } else {
                throw new IOException("Scanner stopped with " + status);
            }
        }
        if (pages.isEmpty()) {
            throw new IOException("The scanner returned no pages");
        }
        return pages;
    }

    private HttpResponse<byte[]> send(HttpRequest request)
            throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    static String settingsXml(
            String source, int resolution, String colorMode, SourceLimits limits) {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <scan:ScanSettings xmlns:pwg="http://www.pwg.org/schemas/2010/12/sm" xmlns:scan="http://schemas.hp.com/imaging/escl/2011/05/03">
          <pwg:Version>2.0</pwg:Version>
          <pwg:ScanRegions>
            <pwg:ScanRegion>
              <pwg:ContentRegionUnits>escl:ThreeHundredthsOfInches</pwg:ContentRegionUnits>
              <pwg:XOffset>0</pwg:XOffset>
              <pwg:YOffset>0</pwg:YOffset>
              <pwg:Width>%d</pwg:Width>
              <pwg:Height>%d</pwg:Height>
            </pwg:ScanRegion>
          </pwg:ScanRegions>
          <pwg:InputSource>%s</pwg:InputSource>
          <scan:ColorMode>%s</scan:ColorMode>
          <scan:XResolution>%d</scan:XResolution>
          <scan:YResolution>%d</scan:YResolution>
          <pwg:DocumentFormat>image/jpeg</pwg:DocumentFormat>
        </scan:ScanSettings>
        """
                .formatted(
                        limits.maxWidth(),
                        limits.maxHeight(),
                        source,
                        colorMode,
                        resolution,
                        resolution);
    }

    static Capabilities parseCapabilities(byte[] xml) throws IOException {
        Document document = parse(xml);

        List<SourceLimits> sources = new ArrayList<>();
        addSource(sources, document, "Platen", "PlatenInputCaps");
        addSource(sources, document, "Feeder", "AdfSimplexInputCaps");

        TreeSet<Integer> resolutions = new TreeSet<>();
        NodeList discrete = document.getElementsByTagNameNS("*", "DiscreteResolution");
        for (int i = 0; i < discrete.getLength(); i++) {
            Integer value = childInt((Element) discrete.item(i), "XResolution");
            if (value != null) {
                resolutions.add(value);
            }
        }

        TreeSet<String> colorModes = new TreeSet<>();
        NodeList modes = document.getElementsByTagNameNS("*", "ColorMode");
        for (int i = 0; i < modes.getLength(); i++) {
            colorModes.add(modes.item(i).getTextContent().strip());
        }

        return new Capabilities(sources, new ArrayList<>(resolutions), new ArrayList<>(colorModes));
    }

    private static void addSource(
            List<SourceLimits> sources, Document document, String source, String capsElement) {
        NodeList caps = document.getElementsByTagNameNS("*", capsElement);
        if (caps.getLength() == 0) {
            return;
        }
        Element element = (Element) caps.item(0);
        Integer width = childInt(element, "MaxWidth");
        Integer height = childInt(element, "MaxHeight");
        if (width != null && height != null) {
            sources.add(new SourceLimits(source, width, height));
        }
    }

    private static Integer childInt(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(nodes.item(0).getTextContent().strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Document parse(byte[] xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Scanner returned unreadable capabilities", e);
        }
    }
}
