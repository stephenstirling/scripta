package scripta.software.core.service.docusign;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocuSignStoreTest {

    @TempDir Path tempDir;

    @Test
    void updatePersistsStateAndReplacesTheFileWithoutLeavingTemporaryFiles() throws IOException {
        Path file = tempDir.resolve("docusign.json");
        DocuSignStore store = new DocuSignStore(file);

        store.update(
                state -> {
                    state.setIntegrationKey("integration-key");
                    state.setRefreshToken("rotated-refresh-token");
                    state.setAccountName("Analytical");
                });

        DocuSignState reloaded = new DocuSignStore(file).read();
        assertThat(reloaded.getIntegrationKey()).isEqualTo("integration-key");
        assertThat(reloaded.getRefreshToken()).isEqualTo("rotated-refresh-token");
        assertThat(reloaded.getAccountName()).isEqualTo("Analytical");
        try (Stream<Path> files = Files.list(tempDir)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .containsExactly("docusign.json");
        }
    }

    @Test
    void updateAndGetReturnsTheUpdatedValue() {
        DocuSignStore store = new DocuSignStore(tempDir.resolve("docusign.json"));

        String accountName =
                store.updateAndGet(
                        state -> {
                            state.setAccountName("Analytical");
                            return state.getAccountName();
                        });

        assertThat(accountName).isEqualTo("Analytical");
        assertThat(store.read().getAccountName()).isEqualTo("Analytical");
    }

    @Test
    void persistedTokenFileUsesOwnerOnlyPosixPermissionsWhereSupported() throws IOException {
        Path file = tempDir.resolve("docusign.json");
        new DocuSignStore(file).update(state -> state.setRefreshToken("secret"));

        try {
            assertThat(Files.getPosixFilePermissions(file))
                    .containsExactlyInAnyOrder(
                            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        } catch (UnsupportedOperationException ignored) {
            // Windows filesystems use ACLs rather than POSIX permissions.
        }
    }
}
