package scripta.software.common.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Programs shipped inside the Windows package, under the folder named by {@code
 * -Dscripta.tools-path}. The packaged launcher cannot extend PATH, so a bare command such as {@code
 * gs} is rewritten to the bundled executable, and child processes get the bundled folders on
 * their PATH so a tool that calls another (OCRmyPDF calling Ghostscript, pdf2image calling
 * Poppler) finds it too. When the property is unset, or a tool is not in the package, every
 * method leaves its input unchanged.
 *
 * <p>The folder layout is written by {@code .github/workflows/windows.yml}; the two must stay in
 * sync.
 */
public final class BundledTools {

    public static final String TOOLS_PATH_PROPERTY = "scripta.tools-path";

    private static final Map<String, String> EXECUTABLES =
            Map.ofEntries(
                    Map.entry("gs", "ghostscript/bin/gswin64c.exe"),
                    Map.entry("gswin64c", "ghostscript/bin/gswin64c.exe"),
                    Map.entry("qpdf", "qpdf/bin/qpdf.exe"),
                    Map.entry("pdftohtml", "poppler/bin/pdftohtml.exe"),
                    Map.entry("pdftoppm", "poppler/bin/pdftoppm.exe"),
                    Map.entry("magick", "imagemagick/magick.exe"),
                    Map.entry("soffice", "libreoffice/program/soffice.exe"),
                    Map.entry("ebook-convert", "calibre/ebook-convert.exe"),
                    Map.entry("weasyprint", "weasyprint/weasyprint.exe"),
                    Map.entry("python", "python/python.exe"),
                    Map.entry("python3", "python/python.exe"),
                    Map.entry("ocrmypdf", "python/ocrmypdf.cmd"),
                    Map.entry("tesseract", "tesseract/tesseract.exe"));

    private static final List<String> PATH_FOLDERS =
            List.of(
                    "ghostscript/bin",
                    "qpdf/bin",
                    "poppler/bin",
                    "imagemagick",
                    "libreoffice/program",
                    "calibre",
                    "weasyprint",
                    "python",
                    "python/Scripts",
                    "tesseract");

    private BundledTools() {}

    /** The bundled tools folder, or null when not running from the Windows package. */
    public static Path root() {
        String value = System.getProperty(TOOLS_PATH_PROPERTY);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Path root = Path.of(value).toAbsolutePath().normalize();
            return Files.isDirectory(root) ? root : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /**
     * The bundled executable for a bare command name ({@code gs}, {@code soffice.exe}), or the
     * command unchanged when it is a path, unknown, or not in the package.
     */
    public static String resolve(String command) {
        Path root = root();
        if (root == null
                || command == null
                || command.isBlank()
                || command.indexOf('/') >= 0
                || command.indexOf('\\') >= 0) {
            return command;
        }
        String key = command.toLowerCase(Locale.ROOT);
        if (key.endsWith(".exe")) {
            key = key.substring(0, key.length() - 4);
        }
        String relative = EXECUTABLES.get(key);
        if (relative == null) {
            return command;
        }
        Path executable = root.resolve(relative).normalize();
        return Files.isRegularFile(executable) ? executable.toString() : command;
    }

    /** True when {@code command} resolves to a bundled executable that exists. */
    public static boolean isBundled(String command) {
        String resolved = resolve(command);
        return resolved != null && !resolved.equals(command);
    }

    /** {@code command} with its executable resolved by {@link #resolve}; the list is not modified. */
    public static List<String> resolveCommand(List<String> command) {
        if (command == null || command.isEmpty()) {
            return command;
        }
        String first = command.getFirst();
        String resolved = resolve(first);
        if (resolved == null || resolved.equals(first)) {
            return command;
        }
        List<String> copy = new ArrayList<>(command);
        copy.set(0, resolved);
        return copy;
    }

    /**
     * Puts the bundled tool folders in front of {@code PATH} in a child process environment, so
     * the bundled copies win over anything else installed on the machine.
     */
    public static void addToPath(Map<String, String> environment) {
        Path root = root();
        if (root == null || environment == null) {
            return;
        }
        List<String> folders = new ArrayList<>();
        for (String folder : PATH_FOLDERS) {
            Path dir = root.resolve(folder).normalize();
            if (Files.isDirectory(dir)) {
                folders.add(dir.toString());
            }
        }
        if (folders.isEmpty()) {
            return;
        }
        String existing = environment.get("PATH");
        String joined = String.join(File.pathSeparator, folders);
        environment.put(
                "PATH",
                existing == null || existing.isBlank()
                        ? joined
                        : joined + File.pathSeparator + existing);
    }
}
