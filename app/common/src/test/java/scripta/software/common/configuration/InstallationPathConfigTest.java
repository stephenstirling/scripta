package scripta.software.common.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

class InstallationPathConfigTest {

    private static final String SEP = File.separator;

    @Test
    void anExplicitOverrideWinsOverEverything() {
        assertEquals(
                "/data" + SEP,
                InstallationPathConfig.resolveBasePath("/data", "/env", true, "C:\\Users\\a\\AppData\\Local"));
    }

    @Test
    void theEnvironmentOverrideIsUsedWhenThePropertyIsBlank() {
        assertEquals("/env" + SEP, InstallationPathConfig.resolveBasePath(" ", "/env", false, null));
    }

    @Test
    void anOverrideKeepsItsTrailingSeparator() {
        assertEquals("/data/", InstallationPathConfig.resolveBasePath("/data/", null, false, null));
        assertEquals("C:\\data\\", InstallationPathConfig.resolveBasePath("C:\\data\\", null, false, null));
    }

    @Test
    void theWindowsLauncherUsesTheUserProfile() {
        assertEquals(
                "C:\\Users\\a\\AppData\\Local" + SEP + "Scripta" + SEP,
                InstallationPathConfig.resolveBasePath(
                        null, null, true, "C:\\Users\\a\\AppData\\Local"));
    }

    @Test
    void withoutTheLauncherFlagTheCurrentFolderIsUsed() {
        assertEquals(
                "." + SEP, InstallationPathConfig.resolveBasePath(null, null, false, "C:\\Users\\a"));
    }

    @Test
    void theLauncherFlagFallsBackToTheCurrentFolderWithoutAProfile() {
        assertEquals("." + SEP, InstallationPathConfig.resolveBasePath(null, null, true, null));
        assertEquals("." + SEP, InstallationPathConfig.resolveBasePath(null, null, true, " "));
    }
}
