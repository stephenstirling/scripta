package scripta.software.core.controller.api.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;

import io.github.pixee.security.Filenames;
import io.swagger.v3.oas.annotations.Operation;

import lombok.extern.slf4j.Slf4j;

import scripta.software.common.annotations.AutoJobPostMapping;
import scripta.software.common.annotations.api.MiscApi;
import scripta.software.common.enumeration.ResourceWeight;
import scripta.software.common.model.api.PDFFile;
import scripta.software.common.model.tool.ToolFormat;
import scripta.software.common.model.tool.ToolIO;
import scripta.software.common.service.CustomPDFDocumentFactory;
import scripta.software.common.util.GeneralUtils;
import scripta.software.common.util.RegexPatternUtils;
import scripta.software.common.util.TempFileManager;
import scripta.software.common.util.WebResponseUtils;
import scripta.software.core.config.swagger.StandardPdfResponse;

@MiscApi
@Slf4j
public class UnlockPDFFormsController {
    private final CustomPDFDocumentFactory pdfDocumentFactory;
    private final TempFileManager tempFileManager;

    public UnlockPDFFormsController(
            CustomPDFDocumentFactory pdfDocumentFactory, TempFileManager tempFileManager) {
        this.pdfDocumentFactory = pdfDocumentFactory;
        this.tempFileManager = tempFileManager;
    }

    @AutoJobPostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            value = "/unlock-pdf-forms",
            resourceWeight = ResourceWeight.SMALL_WEIGHT)
    @StandardPdfResponse
    @ToolIO(produces = ToolFormat.PDF)
    @Operation(
            summary = "Remove read-only property from form fields",
            description = "Removing read-only property from form fields making them fillable")
    public ResponseEntity<Resource> unlockPDFForms(@ModelAttribute PDFFile file) {
        try (PDDocument document = pdfDocumentFactory.load(file)) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                acroForm.setNeedAppearances(true);

                for (PDField field : acroForm.getFieldTree()) {
                    COSDictionary dict = field.getCOSObject();
                    if (dict.containsKey(COSName.getPDFName("Lock"))) {
                        dict.removeItem(COSName.getPDFName("Lock"));
                    }
                    int currentFlags = field.getFieldFlags();
                    if ((currentFlags & 1) == 1) {
                        int newFlags = currentFlags & ~1;
                        field.setFieldFlags(newFlags);
                    }
                }

                COSBase xfaBase = acroForm.getCOSObject().getDictionaryObject(COSName.XFA);
                if (xfaBase != null) {
                    try {
                        var accessReadOnlyPattern =
                                RegexPatternUtils.getInstance().getAccessReadOnlyPattern();
                        if (xfaBase instanceof COSStream xfaStream) {
                            InputStream is = xfaStream.createInputStream();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            is.transferTo(baos);
                            String xml = baos.toString(StandardCharsets.UTF_8);

                            xml = accessReadOnlyPattern.matcher(xml).replaceAll("access=\"open\"");

                            PDStream newStream =
                                    new PDStream(
                                            document,
                                            new ByteArrayInputStream(
                                                    xml.getBytes(StandardCharsets.UTF_8)));
                            acroForm.getCOSObject().setItem(COSName.XFA, newStream.getCOSObject());
                        } else if (xfaBase instanceof COSArray xfaArray) {
                            for (int i = 0; i < xfaArray.size(); i += 2) {
                                COSBase namePart = xfaArray.getObject(i);
                                COSBase streamPart = xfaArray.getObject(i + 1);
                                if (namePart instanceof COSString
                                        && streamPart instanceof COSStream stream) {
                                    InputStream is = stream.createInputStream();
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    is.transferTo(baos);
                                    String xml = baos.toString(StandardCharsets.UTF_8);

                                    xml =
                                            accessReadOnlyPattern
                                                    .matcher(xml)
                                                    .replaceAll("access=\"open\"");

                                    PDStream newStream =
                                            new PDStream(
                                                    document,
                                                    new ByteArrayInputStream(
                                                            xml.getBytes(StandardCharsets.UTF_8)));
                                    xfaArray.set(i + 1, newStream.getCOSObject());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("exception", e);
                    }
                }
            }
            String mergedFileName =
                    GeneralUtils.generateFilename(
                            file.getFileInput().getOriginalFilename(), "_unlocked_forms.pdf");
            return WebResponseUtils.pdfDocToWebResponse(
                    document, Filenames.toSimpleFileName(mergedFileName), tempFileManager);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
