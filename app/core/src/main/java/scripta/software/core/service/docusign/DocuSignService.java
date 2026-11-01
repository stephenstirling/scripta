package scripta.software.core.service.docusign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import scripta.software.core.service.docusign.DocuSignClient.Account;
import scripta.software.core.service.docusign.DocuSignClient.EnvelopeRequest;
import scripta.software.core.service.docusign.DocuSignClient.EnvelopeStatus;
import scripta.software.core.service.docusign.DocuSignClient.Signer;
import scripta.software.core.service.docusign.DocuSignClient.Tokens;
import scripta.software.core.service.docusign.DocuSignState.TrackedEnvelope;

/**
 * Scripta's single DocuSign connection: the integration key, the browser sign-in, the stored
 * tokens, and sending and following envelopes. The open-source build has no user accounts, so
 * there is one connection per installation.
 *
 * <p>Errors the caller can act on are thrown as {@link DocuSignException} with status 400 (bad
 * input) or 409 (not connected / not configured).
 */
@Service
public class DocuSignService {

    public static final String ENVIRONMENT_DEMO = "demo";
    public static final String ENVIRONMENT_PRODUCTION = "production";

    static final int MAX_SIGNERS = 10;
    private static final long SIGN_IN_WINDOW_SECONDS = 600;
    private static final long TOKEN_EXPIRY_MARGIN_SECONDS = 60;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Where the connection stands, for the UI. */
    public record Status(
            boolean configured,
            boolean connected,
            String environment,
            String integrationKey,
            String accountName,
            String userName,
            String email) {}

    private record PendingSignIn(String codeVerifier, long expiresAt) {}

    private final DocuSignStore store;
    private final Function<String, DocuSignClient> clients;
    private final Map<String, PendingSignIn> pendingSignIns = new ConcurrentHashMap<>();

    @Autowired
    public DocuSignService(DocuSignStore store) {
        this(store, environment -> new DocuSignClient(oauthBase(environment)));
    }

    DocuSignService(DocuSignStore store, Function<String, DocuSignClient> clients) {
        this.store = store;
        this.clients = clients;
    }

    public Status status() {
        DocuSignState state = store.read();
        return new Status(
                !state.getIntegrationKey().isBlank(),
                state.getRefreshToken() != null,
                state.getEnvironment(),
                state.getIntegrationKey(),
                state.getAccountName(),
                state.getUserName(),
                state.getEmail());
    }

    /** Saves the integration key and environment; changing either drops the current sign-in. */
    public void configure(String integrationKey, String environment) {
        String key = integrationKey == null ? "" : integrationKey.trim();
        if (!ENVIRONMENT_DEMO.equals(environment) && !ENVIRONMENT_PRODUCTION.equals(environment)) {
            throw new DocuSignException(400, "Environment must be 'demo' or 'production'");
        }
        store.update(
                state -> {
                    boolean changed =
                            !key.equals(state.getIntegrationKey())
                                    || !environment.equals(state.getEnvironment());
                    state.setIntegrationKey(key);
                    state.setEnvironment(environment);
                    if (changed) {
                        clearSignIn(state);
                    }
                });
    }

    /**
     * Starts the browser sign-in and returns the DocuSign page to open. {@code redirectUri} must
     * be registered on the integration key in DocuSign.
     */
    public String beginSignIn(String redirectUri) {
        DocuSignState state = store.read();
        if (state.getIntegrationKey().isBlank()) {
            throw new DocuSignException(409, "Enter your DocuSign integration key first");
        }
        long now = Instant.now().getEpochSecond();
        pendingSignIns.values().removeIf(p -> p.expiresAt() < now);

        String stateToken = randomToken(32);
        String verifier = randomToken(64);
        pendingSignIns.put(stateToken, new PendingSignIn(verifier, now + SIGN_IN_WINDOW_SECONDS));
        return clients.apply(state.getEnvironment())
                .authorizationUrl(
                        state.getIntegrationKey(), redirectUri, stateToken, challenge(verifier));
    }

    /** Finishes the sign-in from DocuSign's redirect; {@code stateToken} must come from {@link #beginSignIn}. */
    public void completeSignIn(String code, String stateToken) {
        PendingSignIn pending = stateToken == null ? null : pendingSignIns.remove(stateToken);
        if (pending == null || pending.expiresAt() < Instant.now().getEpochSecond()) {
            throw new DocuSignException(400, "This sign-in link has expired. Start again.");
        }
        DocuSignState state = store.read();
        DocuSignClient client = clients.apply(state.getEnvironment());
        Tokens tokens =
                client.exchangeCode(state.getIntegrationKey(), code, pending.codeVerifier());
        Account account = client.defaultAccount(tokens.accessToken());
        store.update(
                s -> {
                    applyTokens(s, tokens);
                    s.setAccountId(account.accountId());
                    s.setAccountName(account.accountName());
                    s.setBaseUri(account.baseUri());
                    s.setUserName(account.userName());
                    s.setEmail(account.email());
                });
    }

    /** Forgets the tokens and account; the integration key and sent-envelope list stay. */
    public void disconnect() {
        store.update(DocuSignService::clearSignIn);
    }

    /** Sends {@code pdf} for signature. {@code signaturePage} is 1-based and must exist. */
    public TrackedEnvelope send(
            String fileName,
            byte[] pdf,
            String subject,
            String message,
            List<Signer> signers,
            boolean sequential,
            int signaturePage) {
        if (subject == null || subject.isBlank()) {
            throw new DocuSignException(400, "Enter an email subject for the signers");
        }
        if (signers == null || signers.isEmpty() || signers.size() > MAX_SIGNERS) {
            throw new DocuSignException(400, "Add between 1 and " + MAX_SIGNERS + " signers");
        }
        for (Signer signer : signers) {
            if (signer.name() == null
                    || signer.name().isBlank()
                    || signer.email() == null
                    || !EMAIL.matcher(signer.email().trim()).matches()) {
                throw new DocuSignException(
                        400, "Every signer needs a name and a valid email address");
            }
        }
        Connection connection = connection();
        List<Signer> cleaned =
                signers.stream()
                        .map(s -> new Signer(s.name().trim(), s.email().trim()))
                        .toList();
        String envelopeId =
                connection
                        .client()
                        .sendEnvelope(
                                connection.account(),
                                connection.accessToken(),
                                new EnvelopeRequest(
                                        fileName,
                                        pdf,
                                        subject.trim(),
                                        message,
                                        cleaned,
                                        sequential,
                                        signaturePage));

        TrackedEnvelope tracked = new TrackedEnvelope();
        tracked.setEnvelopeId(envelopeId);
        tracked.setFileName(fileName);
        tracked.setSubject(subject.trim());
        tracked.setSentAt(Instant.now().toString());
        tracked.setSignerEmails(cleaned.stream().map(Signer::email).toList());
        store.update(state -> state.getEnvelopes().add(0, tracked));
        return tracked;
    }

    /** Envelopes sent from this installation, newest first. */
    public List<TrackedEnvelope> sentEnvelopes() {
        return store.read().getEnvelopes();
    }

    public EnvelopeStatus envelopeStatus(String envelopeId) {
        Connection connection = connection();
        return connection
                .client()
                .envelopeStatus(connection.account(), connection.accessToken(), envelopeId);
    }

    /** The signed document with DocuSign's certificate of completion appended. */
    public byte[] signedDocument(String envelopeId) {
        return download(envelopeId, "combined");
    }

    /** DocuSign's certificate of completion on its own. */
    public byte[] certificate(String envelopeId) {
        return download(envelopeId, "certificate");
    }

    private byte[] download(String envelopeId, String documentId) {
        Connection connection = connection();
        return connection
                .client()
                .downloadDocument(
                        connection.account(), connection.accessToken(), envelopeId, documentId);
    }

    private record Connection(DocuSignClient client, Account account, String accessToken) {}

    /** A usable connection, refreshing the access token first when it is about to lapse. */
    private Connection connection() {
        DocuSignState state = store.read();
        if (state.getRefreshToken() == null || state.getAccountId() == null) {
            throw new DocuSignException(409, "Connect DocuSign first");
        }
        DocuSignClient client = clients.apply(state.getEnvironment());
        String accessToken = state.getAccessToken();
        long now = Instant.now().getEpochSecond();
        if (accessToken == null || state.getAccessTokenExpiresAt() - TOKEN_EXPIRY_MARGIN_SECONDS <= now) {
            Tokens tokens;
            try {
                tokens = client.refresh(state.getIntegrationKey(), state.getRefreshToken());
            } catch (DocuSignException e) {
                if (e.getStatus() == 400 || e.getStatus() == 401) {
                    store.update(DocuSignService::clearSignIn);
                    throw new DocuSignException(
                            409, "The DocuSign sign-in has expired. Connect DocuSign again.");
                }
                throw e;
            }
            store.update(s -> applyTokens(s, tokens));
            accessToken = tokens.accessToken();
        }
        Account account =
                new Account(
                        state.getAccountId(),
                        state.getAccountName(),
                        state.getBaseUri(),
                        state.getUserName(),
                        state.getEmail());
        return new Connection(client, account, accessToken);
    }

    private static void applyTokens(DocuSignState state, Tokens tokens) {
        state.setAccessToken(tokens.accessToken());
        if (tokens.refreshToken() != null) {
            state.setRefreshToken(tokens.refreshToken());
        }
        state.setAccessTokenExpiresAt(
                Instant.now().getEpochSecond() + tokens.expiresInSeconds());
    }

    private static void clearSignIn(DocuSignState state) {
        state.setAccessToken(null);
        state.setRefreshToken(null);
        state.setAccessTokenExpiresAt(0);
        state.setAccountId(null);
        state.setAccountName(null);
        state.setBaseUri(null);
        state.setUserName(null);
        state.setEmail(null);
    }

    private static String oauthBase(String environment) {
        return ENVIRONMENT_PRODUCTION.equals(environment)
                ? DocuSignClient.PRODUCTION_OAUTH_BASE
                : DocuSignClient.DEMO_OAUTH_BASE;
    }

    private static String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    /** PKCE S256 challenge, RFC 7636. */
    static String challenge(String verifier) {
        try {
            byte[] hash =
                    MessageDigest.getInstance("SHA-256")
                            .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
