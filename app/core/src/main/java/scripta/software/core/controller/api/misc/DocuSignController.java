package scripta.software.core.controller.api.misc;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import io.github.pixee.security.Filenames;
import io.swagger.v3.oas.annotations.Hidden;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import scripta.software.common.service.CustomPDFDocumentFactory;
import scripta.software.core.service.docusign.DocuSignClient.EnvelopeStatus;
import scripta.software.core.service.docusign.DocuSignClient.Signer;
import scripta.software.core.service.docusign.DocuSignException;
import scripta.software.core.service.docusign.DocuSignService;
import scripta.software.core.service.docusign.DocuSignState.TrackedEnvelope;

/**
 * Send a PDF for signature through the user's own DocuSign account, and follow it afterwards.
 * Hidden from the API docs: it is the backend of one UI tool, not a document-processing endpoint.
 *
 * <p>The browser sign-in redirects to {@code http://localhost:<port>/api/v1/docusign/callback},
 * the only kind of plain-HTTP redirect DocuSign accepts, so signing in works from the machine
 * running Scripta.
 */
@Hidden
@RestController
@RequestMapping("/api/v1/docusign")
@RequiredArgsConstructor
public class DocuSignController {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String CALLBACK_PATH = "/api/v1/docusign/callback";

    private final DocuSignService docuSign;
    private final CustomPDFDocumentFactory pdfDocumentFactory;

    public record ConfigRequest(String integrationKey, String environment) {}

    @GetMapping("/status")
    public DocuSignService.Status status() {
        return docuSign.status();
    }

    @PutMapping("/config")
    public DocuSignService.Status configure(@RequestBody ConfigRequest request) {
        docuSign.configure(request.integrationKey(), request.environment());
        return docuSign.status();
    }

    /**
     * The DocuSign page to open, plus the redirect URI the user must have registered on their
     * integration key. {@code sameMachine} is false when Scripta is being used from another
     * device, where DocuSign's redirect to localhost would land on the wrong machine.
     */
    @GetMapping("/connect")
    public Map<String, Object> connect(HttpServletRequest request) {
        String redirectUri = "http://localhost:" + request.getServerPort() + CALLBACK_PATH;
        String host = request.getServerName();
        boolean sameMachine =
                "localhost".equalsIgnoreCase(host)
                        || "127.0.0.1".equals(host)
                        || "[::1]".equals(host)
                        || "::1".equals(host);
        return Map.of(
                "url", docuSign.beginSignIn(redirectUri),
                "redirectUri", redirectUri,
                "sameMachine", sameMachine);
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {
        if (error != null) {
            return page(
                    400,
                    "DocuSign sign-in was not completed",
                    errorDescription != null ? errorDescription : error);
        }
        if (code == null) {
            return page(400, "DocuSign sign-in was not completed", "No authorization code.");
        }
        try {
            docuSign.completeSignIn(code, state);
        } catch (DocuSignException e) {
            return page(e.getStatus() == 0 ? 502 : e.getStatus(), "DocuSign sign-in failed", e.getMessage());
        }
        return page(
                200,
                "DocuSign is connected",
                "You can close this tab and return to Scripta.");
    }

    @PostMapping("/disconnect")
    public DocuSignService.Status disconnect() {
        docuSign.disconnect();
        return docuSign.status();
    }

    /**
     * @param signers JSON array of {@code {"name": "...", "email": "..."}}
     * @param signaturePage 1-based page for the signature boxes; 0 or less means the last page
     */
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TrackedEnvelope send(
            @RequestParam("fileInput") MultipartFile file,
            @RequestParam String subject,
            @RequestParam(required = false) String message,
            @RequestParam String signers,
            @RequestParam(defaultValue = "false") boolean sequential,
            @RequestParam(defaultValue = "0") int signaturePage)
            throws IOException {
        List<Signer> parsed;
        try {
            parsed = JSON.readValue(signers, new TypeReference<List<Signer>>() {});
        } catch (RuntimeException e) {
            throw new DocuSignException(400, "The signer list could not be read");
        }
        byte[] pdf = file.getBytes();
        int pageCount;
        try (PDDocument document = pdfDocumentFactory.load(pdf, true)) {
            pageCount = document.getNumberOfPages();
        }
        if (signaturePage > pageCount) {
            throw new DocuSignException(
                    400, "The document has " + pageCount + " pages, not " + signaturePage);
        }
        String name = Filenames.toSimpleFileName(file.getOriginalFilename());
        return docuSign.send(
                name == null || name.isBlank() ? "document.pdf" : name,
                pdf,
                subject,
                message,
                parsed,
                sequential,
                signaturePage <= 0 ? pageCount : signaturePage);
    }

    @GetMapping("/envelopes")
    public List<TrackedEnvelope> envelopes() {
        return docuSign.sentEnvelopes();
    }

    @GetMapping("/envelopes/{envelopeId}")
    public EnvelopeStatus envelope(@PathVariable String envelopeId) {
        return docuSign.envelopeStatus(envelopeId);
    }

    @GetMapping("/envelopes/{envelopeId}/signed")
    public ResponseEntity<byte[]> signed(@PathVariable String envelopeId) {
        return pdf(docuSign.signedDocument(envelopeId), "signed-" + shortId(envelopeId) + ".pdf");
    }

    @GetMapping("/envelopes/{envelopeId}/certificate")
    public ResponseEntity<byte[]> certificate(@PathVariable String envelopeId) {
        return pdf(
                docuSign.certificate(envelopeId), "certificate-" + shortId(envelopeId) + ".pdf");
    }

    @ExceptionHandler(DocuSignException.class)
    public ResponseEntity<Map<String, String>> handle(DocuSignException e) {
        int status = e.getStatus() == 400 || e.getStatus() == 409 ? e.getStatus() : 502;
        return ResponseEntity.status(status).body(Map.of("message", e.getMessage()));
    }

    private static String shortId(String envelopeId) {
        return envelopeId.length() > 8 ? envelopeId.substring(0, 8) : envelopeId;
    }

    private static ResponseEntity<byte[]> pdf(byte[] body, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(body);
    }

    private static ResponseEntity<String> page(int status, String title, String detail) {
        String html =
                "<!doctype html><meta charset=\"utf-8\"><title>"
                        + HtmlUtils.htmlEscape(title)
                        + "</title><body style=\"font-family:system-ui;max-width:32rem;margin:4rem auto;padding:0 1rem\"><h1>"
                        + HtmlUtils.htmlEscape(title)
                        + "</h1><p>"
                        + HtmlUtils.htmlEscape(detail)
                        + "</p></body>";
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(html);
    }
}
