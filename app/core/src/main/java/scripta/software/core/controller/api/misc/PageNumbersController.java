package scripta.software.core.controller.api.misc;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import io.github.pixee.security.Filenames;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import scripta.software.common.annotations.AutoJobPostMapping;
import scripta.software.common.annotations.api.MiscApi;
import scripta.software.common.enumeration.ResourceWeight;
import scripta.software.common.model.tool.ToolFormat;
import scripta.software.common.model.tool.ToolIO;
import scripta.software.common.service.CustomPDFDocumentFactory;
import scripta.software.common.util.GeneralUtils;
import scripta.software.common.util.TempFile;
import scripta.software.common.util.TempFileManager;
import scripta.software.common.util.WebResponseUtils;
import scripta.software.core.config.swagger.StandardPdfResponse;
import scripta.software.core.model.api.misc.AddPageNumbersRequest;
import scripta.software.core.utils.PageTextStamper;

@MiscApi
@RequiredArgsConstructor
public class PageNumbersController {

    private final CustomPDFDocumentFactory pdfDocumentFactory;
    private final TempFileManager tempFileManager;

    @AutoJobPostMapping(
            value = "/add-page-numbers",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            resourceWeight = ResourceWeight.SMALL_WEIGHT)
    @StandardPdfResponse
    @ToolIO(produces = ToolFormat.PDF)
    @Operation(
            summary = "Add page numbers to a PDF document",
            description = "This operation takes an input PDF file and adds page numbers to it.")
    public ResponseEntity<Resource> addPageNumbers(@ModelAttribute AddPageNumbersRequest request)
            throws IOException {

        MultipartFile file = request.getFileInput();
        int pageNumber = request.getStartingNumber();
        String pagesToNumber = request.getPagesToNumber();
        String customText = request.getCustomText();
        int zeroPad = request.getZeroPad();
        String formatN = zeroPad > 0 ? "%0" + zeroPad + "d" : "%d";

        try (PDDocument document = pdfDocumentFactory.load(file)) {
            if (pagesToNumber == null || pagesToNumber.isEmpty()) {
                pagesToNumber = "all";
            }
            if (customText == null || customText.isEmpty()) {
                customText = "{n}";
            }

            List<Integer> pagesToNumberList =
                    GeneralUtils.parsePageList(
                            pagesToNumber.split(","), document.getNumberOfPages());

            PageTextStamper stamper =
                    new PageTextStamper(
                            document,
                            request.getFontType(),
                            request.getFontSize(),
                            PageTextStamper.parseColor(request.getFontColor()),
                            PageTextStamper.marginFactor(request.getCustomMargin()));
            String originalName = file.getOriginalFilename();
            String baseName =
                    originalName == null
                            ? ""
                            : GeneralUtils.removeExtension(
                                    Filenames.toSimpleFileName(originalName));

            for (int i : pagesToNumberList) {
                String text =
                        customText
                                .replace("{n}", String.format(Locale.ROOT, formatN, pageNumber))
                                .replace("{total}", String.valueOf(document.getNumberOfPages()))
                                .replace("{filename}", baseName);
                stamper.stamp(document.getPage(i), request.getPosition(), text);
                pageNumber++;
            }

            TempFile tempOut = tempFileManager.createManagedTempFile(".pdf");
            try {
                document.save(tempOut.getFile());
            } catch (IOException e) {
                tempOut.close();
                throw e;
            }

            return WebResponseUtils.pdfFileToWebResponse(
                    tempOut,
                    GeneralUtils.generateFilename(
                            file.getOriginalFilename(), "_page_numbers_added.pdf"));
        }
    }
}
