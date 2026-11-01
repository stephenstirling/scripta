package scripta.software.core.service.docusign;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Thin wrapper over the DocuSign eSignature REST API and its OAuth endpoints. Holds no state and
 * no credentials: every call takes the tokens it needs, so one instance serves any account.
 *
 * <p>Failures surface as {@link DocuSignException}; nothing here retries.
 */
public class DocuSignClient {

    public static final String DEMO_OAUTH_BASE = "https://account-d.docusign.com";
    public static final String PRODUCTION_OAUTH_BASE = "https://account.docusign.com";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** OAuth tokens; {@code refreshToken} is null when DocuSign did not issue a new one. */
    public record Tokens(String accessToken, String refreshToken, long expiresInSeconds) {}

    /** The DocuSign account an access token acts on. {@code baseUri} has no trailing slash. */
    public record Account(
            String accountId, String accountName, String baseUri, String userName, String email) {}

    public record Signer(String name, String email) {}

    /**
     * What to send. Each signer gets a signature box and a date box on {@code signaturePage}
     * (1-based), laid out left to right so several signers do not overlap.
     */
    public record EnvelopeRequest(
            String fileName,
            byte[] pdf,
            String subject,
            String message,
            List<Signer> signers,
            boolean sequential,
            int signaturePage) {}

    public record RecipientStatus(String name, String email, String status) {}

    public record EnvelopeStatus(
            String envelopeId,
            String status,
            String sentAt,
            String completedAt,
            List<RecipientStatus> signers) {}

    private final HttpClient http =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

    private final String oauthBase;

    /** @param oauthBase {@link #DEMO_OAUTH_BASE} or {@link #PRODUCTION_OAUTH_BASE} */
    public DocuSignClient(String oauthBase) {
        this.oauthBase = oauthBase.endsWith("/") ? oauthBase.substring(0, oauthBase.length() - 1) : oauthBase;
    }

    /** Authorization Code Grant with PKCE: a public client, so no secret is involved. */
    public String authorizationUrl(
            String integrationKey, String redirectUri, String state, String codeChallenge) {
        return oauthBase
                + "/oauth/auth?"
                + form(
                        Map.of(
                                "response_type", "code",
                                "scope", "signature",
                                "client_id", integrationKey,
                                "redirect_uri", redirectUri,
                                "state", state,
                                "code_challenge", codeChallenge,
                                "code_challenge_method", "S256"));
    }

    public Tokens exchangeCode(String integrationKey, String code, String codeVerifier) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("client_id", integrationKey);
        body.put("code_verifier", codeVerifier);
        return tokens(postForm("/oauth/token", body));
    }

    public Tokens refresh(String integrationKey, String refreshToken) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("grant_type", "refresh_token");
        body.put("refresh_token", refreshToken);
        body.put("client_id", integrationKey);
        return tokens(postForm("/oauth/token", body));
    }

    /** The user's default account, or the first one when none is flagged as default. */
    public Account defaultAccount(String accessToken) {
        JsonNode info =
                send(
                        HttpRequest.newBuilder(URI.create(oauthBase + "/oauth/userinfo"))
                                .header("Authorization", "Bearer " + accessToken)
                                .GET());
        JsonNode accounts = info.path("accounts");
        JsonNode chosen = null;
        for (JsonNode account : accounts) {
            if (chosen == null || account.path("is_default").asBoolean(false)) {
                chosen = account;
            }
        }
        if (chosen == null) {
            throw new DocuSignException(0, "This DocuSign login has no accounts");
        }
        String baseUri = chosen.path("base_uri").asString("");
        return new Account(
                chosen.path("account_id").asString(""),
                chosen.path("account_name").asString(""),
                baseUri.endsWith("/") ? baseUri.substring(0, baseUri.length() - 1) : baseUri,
                info.path("name").asString(""),
                info.path("email").asString(""));
    }

    /** Creates the envelope and sends it straight away; returns its id. */
    public String sendEnvelope(Account account, String accessToken, EnvelopeRequest request) {
        ObjectNode root = JSON.createObjectNode();
        root.put("emailSubject", request.subject());
        if (request.message() != null && !request.message().isBlank()) {
            root.put("emailBlurb", request.message());
        }
        root.put("status", "sent");

        ObjectNode document = root.putArray("documents").addObject();
        document.put("documentBase64", Base64.getEncoder().encodeToString(request.pdf()));
        document.put("name", request.fileName());
        document.put("fileExtension", "pdf");
        document.put("documentId", "1");

        ArrayNode signers = root.putObject("recipients").putArray("signers");
        for (int i = 0; i < request.signers().size(); i++) {
            Signer signer = request.signers().get(i);
            ObjectNode node = signers.addObject();
            node.put("name", signer.name());
            node.put("email", signer.email());
            node.put("recipientId", String.valueOf(i + 1));
            node.put("routingOrder", String.valueOf(request.sequential() ? i + 1 : 1));
            ObjectNode tabs = node.putObject("tabs");
            int x = 72 + (i % 2) * 250;
            int y = 600 + (i / 2) * 70;
            addTab(tabs.putArray("signHereTabs"), request.signaturePage(), x, y);
            addTab(tabs.putArray("dateSignedTabs"), request.signaturePage(), x, y + 40);
        }

        JsonNode response =
                send(
                        HttpRequest.newBuilder(URI.create(accountUrl(account) + "/envelopes"))
                                .header("Authorization", "Bearer " + accessToken)
                                .header("Content-Type", "application/json")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                JSON.writeValueAsString(root))));
        return response.path("envelopeId").asString("");
    }

    public EnvelopeStatus envelopeStatus(Account account, String accessToken, String envelopeId) {
        JsonNode node =
                send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                accountUrl(account)
                                                        + "/envelopes/"
                                                        + segment(envelopeId)
                                                        + "?include=recipients"))
                                .header("Authorization", "Bearer " + accessToken)
                                .GET());
        List<RecipientStatus> signers = new ArrayList<>();
        for (JsonNode signer : node.path("recipients").path("signers")) {
            signers.add(
                    new RecipientStatus(
                            signer.path("name").asString(""),
                            signer.path("email").asString(""),
                            signer.path("status").asString("")));
        }
        return new EnvelopeStatus(
                envelopeId,
                node.path("status").asString(""),
                node.path("sentDateTime").asString(""),
                node.path("completedDateTime").asString(""),
                signers);
    }

    /**
     * The signed PDF with its certificate of completion appended ({@code combined}), or the
     * certificate alone ({@code certificate}). Only meaningful once the envelope is completed.
     */
    public byte[] downloadDocument(
            Account account, String accessToken, String envelopeId, String documentId) {
        HttpRequest request =
                HttpRequest.newBuilder(
                                URI.create(
                                        accountUrl(account)
                                                + "/envelopes/"
                                                + segment(envelopeId)
                                                + "/documents/"
                                                + segment(documentId)))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "application/pdf")
                        .timeout(REQUEST_TIMEOUT)
                        .GET()
                        .build();
        return execute(request, HttpResponse.BodyHandlers.ofByteArray()).body();
    }

    private static void addTab(ArrayNode tabs, int page, int x, int y) {
        ObjectNode tab = tabs.addObject();
        tab.put("documentId", "1");
        tab.put("pageNumber", String.valueOf(page));
        tab.put("xPosition", String.valueOf(x));
        tab.put("yPosition", String.valueOf(y));
    }

    private static String accountUrl(Account account) {
        return account.baseUri() + "/restapi/v2.1/accounts/" + segment(account.accountId());
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String form(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(
                        e ->
                                URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                                        + "="
                                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private JsonNode postForm(String path, Map<String, String> fields) {
        return send(
                HttpRequest.newBuilder(URI.create(oauthBase + path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form(fields))));
    }

    private static Tokens tokens(JsonNode node) {
        String refresh = node.path("refresh_token").asString("");
        return new Tokens(
                node.path("access_token").asString(""),
                refresh.isEmpty() ? null : refresh,
                node.path("expires_in").asLong(0));
    }

    private JsonNode send(HttpRequest.Builder builder) {
        HttpResponse<String> response =
                execute(
                        builder.timeout(REQUEST_TIMEOUT).build(),
                        HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        return body == null || body.isBlank() ? JSON.createObjectNode() : JSON.readTree(body);
    }

    private <T> HttpResponse<T> execute(
            HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            HttpResponse<T> response = http.send(request, handler);
            if (response.statusCode() / 100 != 2) {
                throw new DocuSignException(response.statusCode(), errorMessage(response));
            }
            return response;
        } catch (IOException e) {
            throw new DocuSignException("Could not reach DocuSign: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocuSignException("Interrupted while calling DocuSign", e);
        }
    }

    private static String errorMessage(HttpResponse<?> response) {
        Object body = response.body();
        String text = body instanceof String s ? s : "";
        try {
            JsonNode node = JSON.readTree(text);
            for (String field : new String[] {"message", "error_description", "error"}) {
                String value = node.path(field).asString("");
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (RuntimeException ignored) {
            // Not JSON: fall through to the status line.
        }
        return "DocuSign answered HTTP " + response.statusCode();
    }
}
