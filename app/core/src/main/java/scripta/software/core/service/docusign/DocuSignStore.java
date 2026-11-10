package scripta.software.core.service.docusign;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import scripta.software.common.configuration.InstallationPathConfig;

/**
 * File-backed holder of {@link DocuSignState}. Reads and writes are serialised, so a token refresh
 * and a concurrent send cannot overwrite each other.
 */
@Component
public class DocuSignStore {

    private static final JsonMapper JSON =
            JsonMapper.builder()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

    private final Path file;

    public DocuSignStore() {
        this(Path.of(InstallationPathConfig.getConfigPath(), "docusign.json"));
    }

    DocuSignStore(Path file) {
        this.file = file;
    }

    /** A copy-safe snapshot: changing it does not change what is stored. */
    public synchronized DocuSignState read() {
        return load();
    }

    /** Applies {@code change} to the stored state and writes the result. */
    public synchronized void update(Consumer<DocuSignState> change) {
        DocuSignState state = load();
        change.accept(state);
        save(state);
    }

    /** Like {@link #update(Consumer)}, returning a value computed from the updated state. */
    public synchronized <T> T updateAndGet(Function<DocuSignState, T> change) {
        DocuSignState state = load();
        T result = change.apply(state);
        save(state);
        return result;
    }

    private DocuSignState load() {
        if (!Files.isRegularFile(file)) {
            return new DocuSignState();
        }
        try {
            return JSON.readValue(Files.readString(file), DocuSignState.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Persist through a same-directory temporary file so a crash cannot leave a partially-written
     * JSON document in place of the last known-good token and envelope state.
     */
    private void save(DocuSignState state) {
        Path parent = file.toAbsolutePath().getParent();
        Path temporaryFile = null;
        try {
            Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
            setOwnerOnly(temporaryFile);

            byte[] json =
                    JSON.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(state)
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(temporaryFile, json, StandardOpenOption.TRUNCATE_EXISTING);
            try (FileChannel channel = FileChannel.open(temporaryFile, StandardOpenOption.WRITE)) {
                channel.force(true);
            }

            try {
                Files.move(
                        temporaryFile,
                        file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Some filesystems do not support atomic replacement. Keep the same-directory
                // staging file and fall back to a replacement move rather than writing in place.
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // The original file remains intact; a leftover temp file is safe to retry.
                }
            }
        }
    }

    private static void setOwnerOnly(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(
                    path, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // Windows uses inherited ACLs instead of POSIX mode bits.
        }
    }
}
