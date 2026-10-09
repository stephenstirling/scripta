package stirling.software.SPDF.controller.api.misc;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import io.github.pixee.security.Filenames;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import stirling.software.SPDF.config.swagger.StandardPdfResponse;
import stirling.software.SPDF.model.api.misc.AddHeaderFooterRequest;
import stirling.software.SPDF.utils.PageTextStamper;
import stirling.software.common.annotations.AutoJobPostMapping;
import stirling.software.common.annotations.api.MiscApi;
import stirling.software.common.enumeration.ResourceWeight;
import stirling.software.common.model.tool.ToolFormat;
import stirling.software.common.model.tool.ToolIO;
import stirling.software.common.service.CustomPDFDocumentFactory;
import stirling.software.common.util.ExceptionUtils;
import stirling.software.common.util.GeneralUtils;
import stirling.software.common.util.TempFile;
import stirling.software.common.util.TempFileManager;
import stirling.software.common.util.WebResponseUtils;

@MiscApi
@RequiredArgsConstructor
public class HeaderFooterController {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    private final CustomPDFDocumentFactory pdfDocumentFactory;
    private final TempFileManager tempFileManager;

    @AutoJobPostMapping(
            value = "/add-header-footer",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            resourceWeight = ResourceWeight.SMALL_WEIGHT)
    @StandardPdfResponse
    @ToolIO(produces = ToolFormat.PDF)
    @Operation(
            summary = "Add headers and footers to a PDF",
            description =
                    "Adds up to six text slots (left, centre and right in the header and the"
                            + " footer) to the selected pages in one pass. Slots support the tokens"
                            + " {n}, {total}, {filename} and {date}. Text follows each page's"
                            + " rotation and crop box, and non-Latin text uses an embedded"
                            + " Unicode font.")
    public ResponseEntity<Resource> addHeaderFooter(@ModelAttribute AddHeaderFooterRequest request)
            throws IOException {
        MultipartFile file = request.getFileInput();
        String[] slots = {
            request.getHeaderLeft(),
            request.getHeaderCenter(),
            request.getHeaderRight(),
            request.getFooterLeft(),
            request.getFooterCenter(),
            request.getFooterRight()
        };
        // Anchor positions for the slots above, in the 1-9 grid PageTextStamper uses.
        int[] positions = {1, 2, 3, 7, 8, 9};

        boolean anyText = false;
        for (String slot : slots) {
            anyText |= slot != null && !slot.isBlank();
        }
        if (!anyText) {
            throw ExceptionUtils.createIllegalArgumentException(
                    "error.headerFooter.empty", "Enter text for at least one header or footer");
        }

        String date = formatToday(request.getDateFormat());
        String originalName = file.getOriginalFilename();
        String baseName =
                originalName == null
                        ? ""
                        : GeneralUtils.removeExtension(Filenames.toSimpleFileName(originalName));

        try (PDDocument document = pdfDocumentFactory.load(file)) {
            int total = document.getNumberOfPages();
            String pages = request.getPagesToApply();
            if (pages == null || pages.isBlank()) {
                pages = "all";
            }
            List<Integer> pageIndexes = GeneralUtils.parsePageList(pages.split(","), total);

            PageTextStamper stamper =
                    new PageTextStamper(
                            document,
                            request.getFontType(),
                            request.getFontSize() > 0 ? request.getFontSize() : 10f,
                            PageTextStamper.parseColor(request.getFontColor()),
                            PageTextStamper.marginFactor(request.getCustomMargin()));

            for (int index : pageIndexes) {
                PDPage page = document.getPage(index);
                String pageNumber = String.valueOf(request.getStartingNumber() + index);
                for (int s = 0; s < slots.length; s++) {
                    String template = slots[s];
                    if (template == null || template.isBlank()) {
                        continue;
                    }
                    String text =
                            template.replace("{n}", pageNumber)
                                    .replace("{total}", String.valueOf(total))
                                    .replace("{filename}", baseName)
                                    .replace("{date}", date);
                    stamper.stamp(page, positions[s], text);
                }
            }

            TempFile tempOut = tempFileManager.createManagedTempFile(".pdf");
            try {
                document.save(tempOut.getFile());
            } catch (IOException e) {
                tempOut.close();
                throw e;
            }
            return WebResponseUtils.pdfFileToWebResponse(
                    tempOut, GeneralUtils.generateFilename(originalName, "_header_footer.pdf"));
        }
    }

    /** Today's date in {@code pattern}; a blank or invalid pattern falls back to ISO dates. */
    static String formatToday(String pattern) {
        LocalDate today = LocalDate.now();
        String effective = pattern == null || pattern.isBlank() ? DEFAULT_DATE_FORMAT : pattern;
        try {
            return today.format(DateTimeFormatter.ofPattern(effective, Locale.ROOT));
        } catch (IllegalArgumentException | DateTimeException e) {
            return today.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT, Locale.ROOT));
        }
    }
}
