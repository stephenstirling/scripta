package scripta.software.core.service.docusign;

/** A DocuSign call that failed, carrying the HTTP status and DocuSign's own explanation. */
public class DocuSignException extends RuntimeException {

    private final int status;

    public DocuSignException(int status, String message) {
        super(message);
        this.status = status;
    }

    public DocuSignException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    /** HTTP status DocuSign answered with, or 0 when no response was received. */
    public int getStatus() {
        return status;
    }
}
