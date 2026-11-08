package scripta.software.core.service.docusign;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Everything Scripta remembers about its DocuSign connection, persisted as one JSON file in the
 * config folder. Holds secrets (the refresh token), so the file is created owner-only where the
 * filesystem allows it.
 */
@Data
public class DocuSignState {

    private String integrationKey = "";

    /** {@code demo} (DocuSign developer sandbox) or {@code production}. */
    private String environment = "demo";

    private String accessToken;
    private String refreshToken;

    /** Epoch seconds at which {@link #accessToken} stops working. */
    private long accessTokenExpiresAt;

    private String accountId;
    private String accountName;
    private String baseUri;
    private String userName;
    private String email;

    private List<TrackedEnvelope> envelopes = new ArrayList<>();

    /** An envelope sent from Scripta, kept so its progress can be followed later. */
    @Data
    public static class TrackedEnvelope {
        private String envelopeId;
        private String fileName;
        private String subject;
        private String sentAt;
        private List<String> signerEmails = new ArrayList<>();
    }
}
