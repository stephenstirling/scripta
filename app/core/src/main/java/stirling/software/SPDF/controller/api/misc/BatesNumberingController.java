package stirling.software.SPDF.controller.api.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import io.github.pixee.security.Filenames;
import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

import stirling.software.SPDF.config.swagger.MultiFileResponse;
import stirling.software.SPDF.model.api.misc.AddBatesNumbersRequest;
import stirling.software.SPDF.utils.PageTextStamper;
import stirling.software.common.annotations.AutoJobPostMapping;
import stirling.software.common.annotations.api.MiscApi;
import stirling.software.common.enumeration.ResourceWeight;
import stirling.software.common.model.tool.ToolArity;
import stirling.software.common.model.tool.ToolFormat;
import stirling.software.common.model.tool.ToolIO;
import stirling.software.common.service.CustomPDFDocumentFactory;
import stirling.software.common.util.ExceptionUtils;
import stirling.software.common.util.GeneralUtils;
import stirling.software.common.util.TempFile;
import stirling.software.common.util.TempFileManager;
import stirling.software.common.util.WebResponseUtils;

/**
 * Bates numbering across a set of documents: every page of every file gets a unique, sequential
 * identifier, and numbering continues from one file to the next in the order the files are sent.
 */
@MiscApi
@RequiredArgsConstructor
public class BatesNumberingController {

    static final int MAX_DIGITS = 15;

    private final CustomPDFDocumentFactory pdfDocumentFactory;
    private final TempFileManager tempFileManager;

    private record StampedFile(TempFile file, String name) {}

    @AutoJobPostMapping(
            value = "/add-bates-numbers",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            resourceWeight = ResourceWeight.MEDIUM_WEIGHT)
    @MultiFileResponse
    @ToolIO(produces = ToolFormat.PDF, arity = ToolArity.MIMO)
    @Operation(
            summary = "Add Bates numbers across one or more PDFs",
            description =
                    "Stamps every page with a prefix, a zero-padded sequential number and a"
                            + " suffix (e.g. ACME000001). Numbering continues across files in the"
                            + " order they are sent, so a production set gets one unbroken"
                            + " sequence. One file returns a PDF; several return a ZIP.")
    public ResponseEntity<Resource> addBatesNumbers(@ModelAttribute AddBatesNumbersRequest request)
            throws IOException {
        MultipartFile[] inputs = request.getFileInput();
        if (inputs == null || inputs.length == 0) {
            throw ExceptionUtils.createIllegalArgumentException(
                    "error.bates.noFiles", "Select at least one PDF to number");
        }
        if (request.getStartNumber() < 0) {
            throw ExceptionUtils.createIllegalArgumentException(
                    "error.bates.negativeStart", "The starting number cannot be negative");
        }
        int digits = Math.max(1, Math.min(MAX_DIGITS, request.getDigits()));
        float fontSize = request.getFontSize() > 0 ? request.getFontSize() : 10f;

        List<StampedFile> outputs = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        long next = request.getStartNumber();
        try {
            for (MultipartFile input : inputs) {
                if (input == null || input.isEmpty()) {
                    continue;
                }
                long first = next;
                TempFile out = tempFileManager.createManagedTempFile(".pdf");
                outputs.add(new StampedFile(out, null));
                try (PDDocument document = pdfDocumentFactory.load(input)) {
                    PageTextStamper stamper =
                            new PageTextStamper(
                                    document,
                                    request.getFontType(),
                                    fontSize,
                                    PageTextStamper.parseColor(request.getFontColor()),
                                    PageTextStamper.marginFactor(request.getCustomMargin()));
                    for (int i = 0; i < document.getNumberOfPages(); i++) {
                        stamper.stamp(
                                document.getPage(i),
                                request.getPosition(),
                                batesLabel(request, digits, next));
                        next++;
                    }
                    document.save(out.getFile());
                }
                String name =
                        uniqueName(outputName(request, input, digits, first, next - 1), usedNames);
                outputs.set(outputs.size() - 1, new StampedFile(out, name));
            }

            if (outputs.isEmpty()) {
                throw ExceptionUtils.createIllegalArgumentException(
                        "error.bates.noFiles", "Select at least one PDF to number");
            }
            if (outputs.size() == 1) {
                StampedFile only = outputs.remove(0);
                return WebResponseUtils.pdfFileToWebResponse(only.file(), only.name());
            }
            TempFile zip = zip(outputs);
            String zipName =
                    GeneralUtils.generateFilename(inputs[0].getOriginalFilename(), "_bates.zip");
            return WebResponseUtils.zipFileToWebResponse(zip, zipName);
        } finally {
            for (StampedFile output : outputs) {
                output.file().close();
            }
        }
    }

    static String batesLabel(AddBatesNumbersRequest request, int digits, long number) {
        String prefix = request.getPrefix() == null ? "" : request.getPrefix();
        String suffix = request.getSuffix() == null ? "" : request.getSuffix();
        return prefix + String.format(Locale.ROOT, "%0" + digits + "d", number) + suffix;
    }

    private static String outputName(
            AddBatesNumbersRequest request,
            MultipartFile input,
            int digits,
            long first,
            long last) {
        if (request.isNameByRange()) {
            String range = batesLabel(request, digits, first);
            if (last > first) {
                range += "-" + batesLabel(request, digits, last);
            }
            return range.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
        }
        String original = input.getOriginalFilename();
        String base =
                original == null
                        ? "document"
                        : GeneralUtils.removeExtension(Filenames.toSimpleFileName(original));
        return base + "_bates.pdf";
    }

    private static String uniqueName(String name, Set<String> used) {
        String candidate = name;
        int n = 2;
        while (!used.add(candidate.toLowerCase(Locale.ROOT))) {
            candidate = GeneralUtils.removeExtension(name) + " (" + n++ + ").pdf";
        }
        return candidate;
    }

    private TempFile zip(List<StampedFile> files) throws IOException {
        TempFile zip = tempFileManager.createManagedTempFile(".zip");
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zip.getPath()))) {
            for (StampedFile file : files) {
                zipOut.putNextEntry(new ZipEntry(file.name()));
                Files.copy(file.file().getPath(), zipOut);
                zipOut.closeEntry();
            }
        } catch (IOException | RuntimeException e) {
            zip.close();
            throw e;
        }
        return zip;
    }
}
