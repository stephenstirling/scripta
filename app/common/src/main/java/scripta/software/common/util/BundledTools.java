package scripta.software.common.util;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Programs shipped inside the Windows package, found through {@code -Dscripta.tools-path}. The
 * launcher cannot extend PATH, so a bundled program is called by absolute path instead. Each tool
 * sits in {@code <tools-path>/<name>/<name>.exe}. Outside the package the property is unset and
 * every lookup falls back to the bare command, which PATH resolves as usual.
 */
public final class BundledTools {

    private BundledTools() {}

    /** The folder of a bundled tool, or null when it is not part of this installation. */
    public static Path dir(String tool) {
        String toolsPath = System.getProperty("scripta.tools-path");
        if (toolsPath == null || toolsPath.isBlank()) {
            return null;
        }
        try {
            Path dir = Path.of(toolsPath).resolve(tool).normalize();
            return Files.isDirectory(dir) ? dir : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /**
     * The absolute path of a bundled tool's executable when {@code command} names one, otherwise
     * {@code command} unchanged. Paths and unknown commands pass through.
     */
    public static String resolve(String command) {
        if (command == null || command.isBlank() || command.contains("/") || command.contains("\\")) {
            return command;
        }
        Path dir = dir(command);
        if (dir == null) {
            return command;
        }
        Path exe = dir.resolve(command + ".exe");
        return Files.isRegularFile(exe) ? exe.toString() : command;
    }
}
