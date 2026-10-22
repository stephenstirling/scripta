package stirling.software.SPDF.service.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import stirling.software.SPDF.service.scanner.EsclScannerClient.Capabilities;
import stirling.software.SPDF.service.scanner.EsclScannerClient.SourceLimits;

class EsclScannerClientTest {

    private static final String CAPABILITIES =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <scan:ScannerCapabilities xmlns:pwg="http://www.pwg.org/schemas/2010/12/sm" xmlns:scan="http://schemas.hp.com/imaging/escl/2011/05/03">
              <scan:Platen>
                <scan:PlatenInputCaps>
                  <scan:MaxWidth>2550</scan:MaxWidth>
                  <scan:MaxHeight>3507</scan:MaxHeight>
                  <scan:SettingProfiles>
                    <scan:SettingProfile>
                      <scan:ColorModes>
                        <scan:ColorMode>RGB24</scan:ColorMode>
                        <scan:ColorMode>Grayscale8</scan:ColorMode>
                      </scan:ColorModes>
                      <scan:SupportedResolutions>
                        <scan:DiscreteResolutions>
                          <scan:DiscreteResolution>
                            <scan:XResolution>300</scan:XResolution>
                            <scan:YResolution>300</scan:YResolution>
                          </scan:DiscreteResolution>
                          <scan:DiscreteResolution>
                            <scan:XResolution>600</scan:XResolution>
                            <scan:YResolution>600</scan:YResolution>
                          </scan:DiscreteResolution>
                        </scan:DiscreteResolutions>
                      </scan:SupportedResolutions>
                    </scan:SettingProfile>
                  </scan:SettingProfiles>
                </scan:PlatenInputCaps>
              </scan:Platen>
              <scan:Adf>
                <scan:AdfSimplexInputCaps>
                  <scan:MaxWidth>2550</scan:MaxWidth>
                  <scan:MaxHeight>4200</scan:MaxHeight>
                </scan:AdfSimplexInputCaps>
              </scan:Adf>
            </scan:ScannerCapabilities>
            """;

    @Test
    void parsesSourcesResolutionsAndColorModes() throws IOException {
        Capabilities capabilities =
                EsclScannerClient.parseCapabilities(CAPABILITIES.getBytes(StandardCharsets.UTF_8));

        assertEquals(
                java.util.List.of(
                        new SourceLimits("Platen", 2550, 3507),
                        new SourceLimits("Feeder", 2550, 4200)),
                capabilities.sources());
        assertEquals(java.util.List.of(300, 600), capabilities.resolutions());
        assertEquals(java.util.List.of("Grayscale8", "RGB24"), capabilities.colorModes());
    }

    @Test
    void rejectsDoctypeDeclarations() {
        String hostile =
                "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>"
                        + "<x>&e;</x>";

        assertThrows(
                IOException.class,
                () ->
                        EsclScannerClient.parseCapabilities(
                                hostile.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void settingsCarryRegionSourceAndResolution() {
        String xml =
                EsclScannerClient.settingsXml(
                        "Feeder", 600, "Grayscale8", new SourceLimits("Feeder", 2550, 4200));

        assertTrue(xml.startsWith("<?xml"));
        assertTrue(xml.contains("<pwg:InputSource>Feeder</pwg:InputSource>"));
        assertTrue(xml.contains("<scan:ColorMode>Grayscale8</scan:ColorMode>"));
        assertTrue(xml.contains("<scan:XResolution>600</scan:XResolution>"));
        assertTrue(xml.contains("<pwg:Width>2550</pwg:Width>"));
        assertTrue(xml.contains("<pwg:Height>4200</pwg:Height>"));
    }
}
