package scripta.software.core.controller.api.misc;

import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import io.github.pixee.security.Filenames;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import scripta.software.common.annotations.AutoJobPostMapping;
import scripta.software.common.annotations.api.MiscApi;
import scripta.software.common.enumeration.ResourceWeight;
import scripta.software.common.model.tool.ToolArity;
import scripta.software.common.model.tool.ToolFormat;
import scripta.software.common.model.tool.ToolIO;
import scripta.software.common.service.CustomPDFDocumentFactory;
import scripta.software.common.util.ExceptionUtils;
import scripta.software.common.util.GeneralUtils;
import scripta.software.common.util.TempFileManager;
import scripta.software.common.util.WebResponseUtils;
import scripta.software.core.config.swagger.StandardPdfResponse;
import scripta.software.core.model.api.misc.CreatePortfolioRequest;
import scripta.software.core.model.api.misc.FlattenPortfolioRequest;
import scripta.software.core.service.PortfolioServiceInterface;

@MiscApi
@Slf4j
@RequiredArgsConstructor
public class PortfolioController {

    private final CustomPDFDocumentFactory pdfDocumentFactory;

    private final PortfolioServiceInterface portfolioService;

    private final TempFileManager tempFileManager;

    @AutoJobPostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            value = "/create-portfolio",
            resourceWeight = ResourceWeight.SMALL_WEIGHT)
    @StandardPdfResponse
    @ToolIO(accepts = ToolFormat.ANY, produces = ToolFormat.PDF, arity = ToolArity.MISO)
    @Operation(
            summary = "Create a PDF Portfolio",
            description =
                    "This endpoint bundles one or more files into an Adobe PDF Portfolio"
                            + " (a PDF with a /Collection dictionary) behind a cover page.")
    public ResponseEntity<Resource> createPortfolio(@ModelAttribute CreatePortfolioRequest request)
            throws Exception {
        List<MultipartFile> files = request.getFiles();
        validatePortfolioFiles(files);

        try (PDDocument document =
                portfolioService.createPortfolio(files, request.getCoverTitle())) {
            return WebResponseUtils.pdfDocToWebResponse(document, "portfolio.pdf", tempFileManager);
        }
    }

    private void validatePortfolioFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw ExceptionUtils.createIllegalArgumentException(
                    "error.portfolioFilesRequired", "At least one file is required");
        }

        final long maxFileSize = 50L * 1024 * 1024; // 50 MB per file
        final long maxTotalSize = 200L * 1024 * 1024; // 200 MB total

        long totalSize = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw ExceptionUtils.createIllegalArgumentException(
                        "error.attachmentEmpty", "Portfolio files cannot be null or empty");
            }
            if (file.getSize() > maxFileSize) {
                throw ExceptionUtils.createIllegalArgumentException(
                        "error.attachmentTooLarge",
                        "File ''{0}'' exceeds maximum size of {1} bytes",
                        file.getOriginalFilename(),
                        maxFileSize);
            }
            totalSize += file.getSize();
        }

        if (totalSize > maxTotalSize) {
            throw ExceptionUtils.createIllegalArgumentException(
                    "error.totalAttachmentsTooLarge",
                    "Total size {0} exceeds maximum of {1} bytes",
                    totalSize,
                    maxTotalSize);
        }
    }

    @AutoJobPostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            value = "/flatten-portfolio",
            resourceWeight = ResourceWeight.SMALL_WEIGHT)
    @StandardPdfResponse
    @ToolIO(produces = ToolFormat.PDF)
    @Operation(
            summary = "Flatten a PDF Portfolio",
            description =
                    "This endpoint removes the /Collection wrapper from a PDF Portfolio so it opens"
                            + " as a standard PDF, keeping the bundled files as attachments.")
    public ResponseEntity<Resource> flattenPortfolio(
            @ModelAttribute FlattenPortfolioRequest request) throws Exception {
        try (PDDocument document = pdfDocumentFactory.load(request, false)) {
            if (!portfolioService.isPortfolio(document)) {
                throw ExceptionUtils.createIllegalArgumentException(
                        "error.notAPortfolio",
                        "The provided PDF is not a portfolio and cannot be flattened");
            }
            portfolioService.flattenPortfolio(document);

            return WebResponseUtils.pdfDocToWebResponse(
                    document,
                    GeneralUtils.generateFilename(
                            Filenames.toSimpleFileName(
                                    request.getFileInput().getOriginalFilename()),
                            "_flattened.pdf"),
                    tempFileManager);
        }
    }
}
