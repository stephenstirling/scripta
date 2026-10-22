package stirling.software.SPDF.controller.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import stirling.software.SPDF.service.scanner.EsclScannerClient;
import stirling.software.SPDF.service.scanner.EsclScannerClient.Capabilities;
import stirling.software.SPDF.service.scanner.EsclScannerClient.ScannedPage;
import stirling.software.SPDF.service.scanner.EsclScannerClient.SourceLimits;
import stirling.software.SPDF.service.scanner.ScannerHostGuard;
import stirling.software.SPDF.service.scanner.ScannerHostGuard.Target;
import stirling.software.common.util.TempFile;
import stirling.software.common.util.TempFileManager;
import stirling.software.common.util.WebResponseUtils;

@RestController
@RequestMapping("/api/v1/scan")
@Tag(name = "Scanner", description = "Scan pages from a network scanner (eSCL/AirScan) into a PDF.")
@RequiredArgsConstructor
public class ScannerController {

    private static final Map<String, String> COLOR_MODES =
            Map.of("color", "RGB24", "gray", "Grayscale8", "bw", "BlackAndWhite1");

    private static final float POINTS_PER_INCH = 72f;

    private final EsclScannerClient scannerClient;
    private final TempFileManager tempFileManager;

    @GetMapping("/capabilities")
    @Operation(
            summary = "Read a network scanner's capabilities",
            description =
                    "Returns the input sources, resolutions and color modes of the scanner at the"
                            + " given private-network address.")
    public ResponseEntity<Capabilities> capabilities(@RequestParam("host") String host)
            throws InterruptedException {
        Target target = ScannerHostGuard.resolve(host);
        try {
            return ResponseEntity.ok(scannerClient.capabilities(target));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @PostMapping("/scan-to-pdf")
    @Operation(
            summary = "Scan pages from a network scanner into a PDF",
            description =
                    "Scans every page the chosen source produces and returns them as one PDF.")
    public ResponseEntity<Resource> scanToPdf(
            @RequestParam("host") String host,
            @RequestParam(value = "source", defaultValue = "Platen") String source,
            @RequestParam(value = "resolution", defaultValue = "300") int resolution,
            @RequestParam(value = "colorMode", defaultValue = "color") String colorMode)
            throws IOException, InterruptedException {
        String esclColorMode = COLOR_MODES.get(colorMode);
        if (esclColorMode == null) {
            throw new IllegalArgumentException("colorMode must be color, gray or bw");
        }
        if (!"Platen".equals(source) && !"Feeder".equals(source)) {
            throw new IllegalArgumentException("source must be Platen or Feeder");
        }
        if (resolution < 75 || resolution > 1200) {
            throw new IllegalArgumentException("resolution must be between 75 and 1200");
        }

        Target target = ScannerHostGuard.resolve(host);
        List<ScannedPage> pages;
        try {
            SourceLimits limits = limitsFor(scannerClient.capabilities(target), source);
            pages = scannerClient.scan(target, source, resolution, esclColorMode, limits);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }

        TempFile output = new TempFile(tempFileManager, ".pdf");
        boolean transferred = false;
        try {
            try (PDDocument document = new PDDocument()) {
                for (ScannedPage page : pages) {
                    addPage(document, page);
                }
                document.save(output.getFile());
            }
            ResponseEntity<Resource> response =
                    WebResponseUtils.pdfFileToWebResponse(output, "scan.pdf");
            transferred = true;
            return response;
        } finally {
            if (!transferred) {
                output.close();
            }
        }
    }

    private static SourceLimits limitsFor(Capabilities capabilities, String source) {
        return capabilities.sources().stream()
                .filter(limits -> limits.source().equals(source))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "This scanner has no " + source + " source"));
    }

    private static void addPage(PDDocument document, ScannedPage scanned) throws IOException {
        PDImageXObject image = JPEGFactory.createFromByteArray(document, scanned.jpeg());
        float width = image.getWidth() * POINTS_PER_INCH / scanned.resolution();
        float height = image.getHeight() * POINTS_PER_INCH / scanned.resolution();
        PDPage page = new PDPage(new PDRectangle(width, height));
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.drawImage(image, 0, 0, width, height);
        }
    }
}
