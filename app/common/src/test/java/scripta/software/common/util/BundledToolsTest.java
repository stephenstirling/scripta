package scripta.software.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundledToolsTest {

    @TempDir Path tools;

    private String previous;

    @BeforeEach
    void remember() {
        previous = System.getProperty(BundledTools.TOOLS_PATH_PROPERTY);
    }

    @AfterEach
    void restore() {
        if (previous == null) {
            System.clearProperty(BundledTools.TOOLS_PATH_PROPERTY);
        } else {
            System.setProperty(BundledTools.TOOLS_PATH_PROPERTY, previous);
        }
    }

    private Path place(String relative) throws IOException {
        Path file = tools.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "");
        return file;
    }

    @Test
    void leavesCommandsAloneOutsideThePackage() {
        System.clearProperty(BundledTools.TOOLS_PATH_PROPERTY);
        assertEquals("gs", BundledTools.resolve("gs"));
        List<String> command = List.of("qpdf", "--version");
        assertSame(command, BundledTools.resolveCommand(command));
        Map<String, String> env = new HashMap<>(Map.of("PATH", "C:\\Windows"));
        BundledTools.addToPath(env);
        assertEquals("C:\\Windows", env.get("PATH"));
    }

    @Test
    void resolvesBareNamesToBundledExecutables() throws IOException {
        Path gs = place("ghostscript/bin/gswin64c.exe");
        Path soffice = place("libreoffice/program/soffice.exe");
        System.setProperty(BundledTools.TOOLS_PATH_PROPERTY, tools.toString());

        assertEquals(gs.toString(), BundledTools.resolve("gs"));
        assertEquals(soffice.toString(), BundledTools.resolve("soffice.exe"));
        assertEquals(soffice.toString(), BundledTools.resolve("SOFFICE"));
        assertTrue(BundledTools.isBundled("gs"));
        assertEquals(
                List.of(gs.toString(), "-dBATCH"),
                BundledTools.resolveCommand(List.of("gs", "-dBATCH")));
    }

    @Test
    void resolvesThroughAToolsPathWithDotDot() throws IOException {
        Path qpdf = place("qpdf/bin/qpdf.exe");
        Files.createDirectories(tools.resolve("app"));
        System.setProperty(
                BundledTools.TOOLS_PATH_PROPERTY, tools.resolve("app").resolve("..").toString());

        assertEquals(qpdf.toString(), BundledTools.resolve("qpdf"));
    }

    @Test
    void leavesUnknownMissingAndPathCommandsAlone() throws IOException {
        place("ghostscript/bin/gswin64c.exe");
        System.setProperty(BundledTools.TOOLS_PATH_PROPERTY, tools.toString());

        assertEquals("fontforge", BundledTools.resolve("fontforge"));
        assertEquals("magick", BundledTools.resolve("magick"));
        assertFalse(BundledTools.isBundled("magick"));
        assertEquals("C:\\other\\gs.exe", BundledTools.resolve("C:\\other\\gs.exe"));
        assertEquals("/usr/bin/gs", BundledTools.resolve("/usr/bin/gs"));
        assertEquals(null, BundledTools.resolve(null));
    }

    @Test
    void putsExistingToolFoldersFirstOnPath() throws IOException {
        place("ghostscript/bin/gswin64c.exe");
        place("tesseract/tesseract.exe");
        System.setProperty(BundledTools.TOOLS_PATH_PROPERTY, tools.toString());
        Map<String, String> env = new HashMap<>(Map.of("PATH", "C:\\Windows"));

        BundledTools.addToPath(env);

        String expected =
                tools.resolve("ghostscript/bin").normalize()
                        + File.pathSeparator
                        + tools.resolve("tesseract").normalize()
                        + File.pathSeparator
                        + "C:\\Windows";
        assertEquals(expected, env.get("PATH"));
    }
}
