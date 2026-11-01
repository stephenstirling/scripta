package scripta.software.core.service.docusign;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private void save(DocuSignState state) {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                createOwnerOnly(file);
            }
            Files.writeString(file, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(state));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void createOwnerOnly(Path path) throws IOException {
        try {
            Files.createFile(
                    path,
                    PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString("rw-------")));
        } catch (UnsupportedOperationException e) {
            Files.createFile(path);
        }
    }
}
