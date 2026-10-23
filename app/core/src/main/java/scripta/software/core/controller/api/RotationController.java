package scripta.software.core.controller.api;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import scripta.software.common.annotations.AutoJobPostMapping;
import scripta.software.common.annotations.api.GeneralApi;
import scripta.software.common.enumeration.ResourceWeight;
import scripta.software.common.model.tool.ToolFormat;
import scripta.software.common.model.tool.ToolIO;
import scripta.software.common.service.CustomPDFDocumentFactory;
import scripta.software.common.util.ExceptionUtils;
import scripta.software.common.util.GeneralUtils;
import scripta.software.common.util.TempFileManager;
import scripta.software.common.util.WebResponseUtils;
import scripta.software.core.config.swagger.StandardPdfResponse;
import scripta.software.core.model.api.general.RotatePDFRequest;

@GeneralApi
@RequiredArgsConstructor
public class RotationController {

    private final CustomPDFDocumentFactory pdfDocumentFactory;
    private final TempFileManager tempFileManager;

    @AutoJobPostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            value = "/rotate-pdf",
            resourceWeight = ResourceWeight.SMALL_WEIGHT)
    @StandardPdfResponse
    @ToolIO(produces = ToolFormat.PDF)
    @Operation(
            summary = "Rotate a PDF file",
            description =
                    "This endpoint rotates a given PDF file by a specified angle. The angle must be"
                            + " a multiple of 90.")
    public ResponseEntity<Resource> rotatePDF(@ModelAttribute RotatePDFRequest request)
            throws IOException {
        MultipartFile pdfFile = request.getFileInput();
        Integer angle = request.getAngle();

        // Validate the angle is a multiple of 90
        if (angle % 90 != 0) {
            throw ExceptionUtils.createIllegalArgumentException(
                    "error.angleNotMultipleOf90", "Angle must be a multiple of 90");
        }

        // Load the PDF document with proper resource management
        try (PDDocument document = pdfDocumentFactory.load(request)) {

            // Get the list of pages in the document
            PDPageTree pages = document.getPages();

            for (PDPage page : pages) {
                page.setRotation(page.getRotation() + angle);
            }

            // Return the rotated PDF as a response
            return WebResponseUtils.pdfDocToWebResponse(
                    document,
                    GeneralUtils.generateFilename(pdfFile.getOriginalFilename(), "_rotated.pdf"),
                    tempFileManager);
        }
    }
}
