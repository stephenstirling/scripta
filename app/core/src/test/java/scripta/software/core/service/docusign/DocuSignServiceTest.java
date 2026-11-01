package scripta.software.core.service.docusign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import scripta.software.core.service.docusign.DocuSignClient.Signer;
import scripta.software.core.service.docusign.DocuSignState.TrackedEnvelope;

/** Runs the service against a fake DocuSign served from this JVM. */
class DocuSignServiceTest {

    private record Call(String method, String path, String authorization, String body) {}

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String REDIRECT = "http://localhost:8080/api/v1/docusign/callback";

    @TempDir Path tempDir;

    private HttpServer server;
    private String base;
    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private final AtomicInteger tokenCounter = new AtomicInteger();
    private volatile int tokenStatus = 200;
    private volatile long expiresIn = 3600;
    private DocuSignService service;
    private DocuSignStore store;

    @BeforeEach
    void startFakeDocuSign() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/", this::handle);
        server.start();
        store = new DocuSignStore(tempDir.resolve("docusign.json"));
        service = new DocuSignService(store, environment -> new DocuSignClient(base));
    }

    @AfterEach
    void stopFakeDocuSign() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String path = exchange.getRequestURI().getPath();
        calls.add(
                new Call(
                        exchange.getRequestMethod(),
                        path,
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        body));

        int status = 200;
        byte[] response;
        String type = "application/json";
        if (path.equals("/oauth/token")) {
            status = tokenStatus;
            response =
                    status == 200
                            ? json(
                                    "{\"access_token\":\"access-%d\",\"refresh_token\":\"refresh-%d\",\"expires_in\":%d}"
                                            .formatted(
                                                    tokenCounter.incrementAndGet(),
                                                    tokenCounter.get(),
                                                    expiresIn))
                            : json("{\"error\":\"invalid_grant\",\"error_description\":\"expired\"}");
        } else if (path.equals("/oauth/userinfo")) {
            response =
                    json(
                            "{\"name\":\"Ada Lovelace\",\"email\":\"ada@example.com\",\"accounts\":["
                                    + "{\"account_id\":\"other\",\"is_default\":false,\"account_name\":\"Other\",\"base_uri\":\""
                                    + base
                                    + "\"},"
                                    + "{\"account_id\":\"ACC\",\"is_default\":true,\"account_name\":\"Analytical\",\"base_uri\":\""
                                    + base
                                    + "/\"}]}");
        } else if (path.equals("/restapi/v2.1/accounts/ACC/envelopes")) {
            response = json("{\"envelopeId\":\"ENV-1\",\"status\":\"sent\"}");
        } else if (path.equals("/restapi/v2.1/accounts/ACC/envelopes/ENV-1")) {
            response =
                    json(
                            "{\"status\":\"completed\",\"sentDateTime\":\"2026-11-01T10:00:00Z\","
                                    + "\"completedDateTime\":\"2026-11-01T11:00:00Z\",\"recipients\":{\"signers\":["
                                    + "{\"name\":\"Grace\",\"email\":\"grace@example.com\",\"status\":\"completed\"}]}}");
        } else if (path.equals("/restapi/v2.1/accounts/ACC/envelopes/ENV-1/documents/combined")) {
            type = "application/pdf";
            response = "%PDF-signed".getBytes(StandardCharsets.US_ASCII);
        } else {
            status = 404;
            response = json("{\"message\":\"no such route\"}");
        }
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static byte[] json(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, String> query(String url) {
        Map<String, String> result = new HashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            result.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return result;
    }

    private Map<String, String> formBody(Call call) {
        Map<String, String> result = new HashMap<>();
        for (String pair : call.body().split("&")) {
            String[] kv = pair.split("=", 2);
            result.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return result;
    }

    private Call lastCall(String path) {
        List<Call> matching = new ArrayList<>(calls.stream().filter(c -> c.path().equals(path)).toList());
        assertThat(matching).as("calls to " + path).isNotEmpty();
        return matching.get(matching.size() - 1);
    }

    private void connect() {
        service.configure("key-123", "demo");
        Map<String, String> params = query(service.beginSignIn(REDIRECT));
        service.completeSignIn("the-code", params.get("state"));
    }

    @Test
    void signInNeedsAnIntegrationKey() {
        assertThatThrownBy(() -> service.beginSignIn(REDIRECT))
                .isInstanceOf(DocuSignException.class)
                .extracting(e -> ((DocuSignException) e).getStatus())
                .isEqualTo(409);
    }

    @Test
    void signInUsesPkceAndStoresTheDefaultAccount() {
        service.configure("key-123", "demo");
        String url = service.beginSignIn(REDIRECT);
        Map<String, String> params = query(url);

        assertThat(url).startsWith(base + "/oauth/auth?");
        assertThat(params)
                .containsEntry("client_id", "key-123")
                .containsEntry("redirect_uri", REDIRECT)
                .containsEntry("scope", "signature")
                .containsEntry("code_challenge_method", "S256");

        service.completeSignIn("the-code", params.get("state"));

        Map<String, String> tokenRequest = formBody(lastCall("/oauth/token"));
        assertThat(tokenRequest)
                .containsEntry("grant_type", "authorization_code")
                .containsEntry("code", "the-code")
                .containsEntry("client_id", "key-123");
        assertThat(DocuSignService.challenge(tokenRequest.get("code_verifier")))
                .isEqualTo(params.get("code_challenge"));

        DocuSignService.Status status = service.status();
        assertThat(status.connected()).isTrue();
        assertThat(status.accountName()).isEqualTo("Analytical");
        assertThat(status.email()).isEqualTo("ada@example.com");
    }

    @Test
    void anUnknownOrReusedStateIsRejected() {
        service.configure("key-123", "demo");
        String state = query(service.beginSignIn(REDIRECT)).get("state");
        service.completeSignIn("code", state);

        assertThatThrownBy(() -> service.completeSignIn("code", state))
                .isInstanceOf(DocuSignException.class)
                .extracting(e -> ((DocuSignException) e).getStatus())
                .isEqualTo(400);
        assertThatThrownBy(() -> service.completeSignIn("code", "made-up"))
                .isInstanceOf(DocuSignException.class);
    }

    @Test
    void changingTheKeyDropsTheConnection() {
        connect();
        service.configure("another-key", "demo");

        assertThat(service.status().connected()).isFalse();
        assertThat(service.status().integrationKey()).isEqualTo("another-key");
    }

    @Test
    void sendPostsTheDocumentWithSignatureBoxesAndRemembersTheEnvelope() throws Exception {
        connect();
        byte[] pdf = "%PDF-1.7 test".getBytes(StandardCharsets.US_ASCII);

        TrackedEnvelope tracked =
                service.send(
                        "contract.pdf",
                        pdf,
                        " Please sign ",
                        "Thanks",
                        List.of(
                                new Signer("Grace", "grace@example.com"),
                                new Signer("Alan", "alan@example.com")),
                        true,
                        3);

        assertThat(tracked.getEnvelopeId()).isEqualTo("ENV-1");
        assertThat(service.sentEnvelopes()).hasSize(1);
        assertThat(service.sentEnvelopes().get(0).getSignerEmails())
                .containsExactly("grace@example.com", "alan@example.com");

        Call call = lastCall("/restapi/v2.1/accounts/ACC/envelopes");
        assertThat(call.authorization()).isEqualTo("Bearer access-1");
        JsonNode body = JSON.readTree(call.body());
        assertThat(body.path("status").asString("")).isEqualTo("sent");
        assertThat(body.path("emailSubject").asString("")).isEqualTo("Please sign");
        assertThat(body.path("documents").get(0).path("documentBase64").asString(""))
                .isEqualTo(Base64.getEncoder().encodeToString(pdf));
        JsonNode signers = body.path("recipients").path("signers");
        assertThat(signers.get(0).path("routingOrder").asString("")).isEqualTo("1");
        assertThat(signers.get(1).path("routingOrder").asString("")).isEqualTo("2");
        JsonNode sign = signers.get(1).path("tabs").path("signHereTabs").get(0);
        assertThat(sign.path("pageNumber").asString("")).isEqualTo("3");
        assertThat(signers.get(0).path("tabs").path("signHereTabs").get(0).path("xPosition").asString(""))
                .isNotEqualTo(sign.path("xPosition").asString(""));
    }

    @Test
    void sendRejectsBadSignersBeforeCallingDocuSign() {
        connect();
        int callsBefore = calls.size();

        assertThatThrownBy(
                        () ->
                                service.send(
                                        "a.pdf",
                                        new byte[] {1},
                                        "Subject",
                                        null,
                                        List.of(new Signer("Grace", "not-an-email")),
                                        false,
                                        1))
                .isInstanceOf(DocuSignException.class)
                .extracting(e -> ((DocuSignException) e).getStatus())
                .isEqualTo(400);
        assertThatThrownBy(
                        () -> service.send("a.pdf", new byte[] {1}, "Subject", null, List.of(), false, 1))
                .isInstanceOf(DocuSignException.class);
        assertThat(calls).hasSize(callsBefore);
    }

    @Test
    void sendBeforeConnectingAsksToConnect() {
        assertThatThrownBy(
                        () ->
                                service.send(
                                        "a.pdf",
                                        new byte[] {1},
                                        "Subject",
                                        null,
                                        List.of(new Signer("Grace", "grace@example.com")),
                                        false,
                                        1))
                .isInstanceOf(DocuSignException.class)
                .extracting(e -> ((DocuSignException) e).getStatus())
                .isEqualTo(409);
    }

    @Test
    void anExpiredAccessTokenIsRefreshedAndTheNewRefreshTokenKept() {
        expiresIn = 0;
        connect();

        service.envelopeStatus("ENV-1");

        Map<String, String> refresh = formBody(lastCall("/oauth/token"));
        assertThat(refresh)
                .containsEntry("grant_type", "refresh_token")
                .containsEntry("refresh_token", "refresh-1");
        assertThat(lastCall("/restapi/v2.1/accounts/ACC/envelopes/ENV-1").authorization())
                .isEqualTo("Bearer access-2");
        assertThat(store.read().getRefreshToken()).isEqualTo("refresh-2");
    }

    @Test
    void aRefreshDocuSignRefusesDisconnectsAndAsksToReconnect() {
        expiresIn = 0;
        connect();
        tokenStatus = 400;

        assertThatThrownBy(() -> service.envelopeStatus("ENV-1"))
                .isInstanceOf(DocuSignException.class)
                .extracting(e -> ((DocuSignException) e).getStatus())
                .isEqualTo(409);
        assertThat(service.status().connected()).isFalse();
    }

    @Test
    void envelopeStatusListsEachSigner() {
        connect();

        DocuSignClient.EnvelopeStatus status = service.envelopeStatus("ENV-1");

        assertThat(status.status()).isEqualTo("completed");
        assertThat(status.completedAt()).isEqualTo("2026-11-01T11:00:00Z");
        assertThat(status.signers())
                .containsExactly(
                        new DocuSignClient.RecipientStatus(
                                "Grace", "grace@example.com", "completed"));
    }

    @Test
    void signedDocumentIsTheCombinedPdf() {
        connect();

        assertThat(new String(service.signedDocument("ENV-1"), StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-signed");
    }

    @Test
    void docuSignErrorsCarryItsMessage() {
        connect();

        assertThatThrownBy(() -> service.envelopeStatus("ENV-MISSING"))
                .isInstanceOf(DocuSignException.class)
                .hasMessage("no such route");
    }

    @Test
    void tokensAreStoredOnlyInTheConfigFile() throws IOException {
        connect();

        String saved = java.nio.file.Files.readString(tempDir.resolve("docusign.json"));
        assertThat(saved).contains("refresh-1").contains("key-123");
        assertThat(Instant.ofEpochSecond(store.read().getAccessTokenExpiresAt()))
                .isAfter(Instant.now());
    }
}
