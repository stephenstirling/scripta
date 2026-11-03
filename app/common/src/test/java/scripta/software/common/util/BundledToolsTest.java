package scripta.software.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundledToolsTest {

    private static final String PROPERTY = "scripta.tools-path";

    @TempDir Path tools;

    @AfterEach
    void clearProperty() {
        System.clearProperty(PROPERTY);
    }

    @Test
    void withoutTheLauncherPropertyEveryCommandPassesThrough() {
        assertEquals("qpdf", BundledTools.resolve("qpdf"));
        assertNull(BundledTools.dir("qpdf"));
    }

    @Test
    void aBundledExecutableIsCalledByAbsolutePath() throws IOException {
        Files.createDirectories(tools.resolve("qpdf"));
        Path exe = Files.createFile(tools.resolve("qpdf").resolve("qpdf.exe"));
        System.setProperty(PROPERTY, tools.toString());

        assertEquals(exe.toString(), BundledTools.resolve("qpdf"));
    }

    @Test
    void aCommandThatIsNotBundledFallsBackToThePath() throws IOException {
        Files.createDirectories(tools.resolve("qpdf"));
        System.setProperty(PROPERTY, tools.toString());

        assertEquals("qpdf", BundledTools.resolve("qpdf"));
        assertEquals("gs", BundledTools.resolve("gs"));
    }

    @Test
    void pathsAndBlankCommandsAreLeftAlone() {
        System.setProperty(PROPERTY, tools.toString());

        assertEquals("C:\\tools\\qpdf.exe", BundledTools.resolve("C:\\tools\\qpdf.exe"));
        assertEquals("/usr/bin/qpdf", BundledTools.resolve("/usr/bin/qpdf"));
        assertEquals("", BundledTools.resolve(""));
        assertNull(BundledTools.resolve(null));
    }
}
