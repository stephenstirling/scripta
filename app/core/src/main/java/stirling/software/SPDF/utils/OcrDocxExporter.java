package stirling.software.SPDF.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/**
 * Builds an editable Word file from the text layer of an OCR'd PDF.
 *
 * <p>Output is text only: paragraphs and page breaks survive, but images, tables, fonts and column
 * layout do not, because the OCR text layer carries none of them.
 */
public final class OcrDocxExporter {

    // Not a character OCR text produces, so it cannot be confused with page content.
    private static final String PARAGRAPH_END = " ";

    private OcrDocxExporter() {}

    /**
     * Reads the text layer of {@code pdf} and writes it to {@code docx}, one page break per page.
     */
    public static void export(Path pdf, Path docx) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile());
                XWPFDocument word = new XWPFDocument();
                OutputStream out = Files.newOutputStream(docx)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setParagraphEnd(PARAGRAPH_END);

            int pages = document.getNumberOfPages();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                for (String paragraph : toParagraphs(stripper.getText(document))) {
                    XWPFParagraph docxParagraph = word.createParagraph();
                    docxParagraph.createRun().setText(paragraph);
                }
                if (page < pages) {
                    word.createParagraph().createRun().addBreak(BreakType.PAGE);
                }
            }
            word.write(out);
        }
    }

    /** Splits stripper output on detected paragraph ends, joining wrapped lines with a space. */
    static List<String> toParagraphs(String pageText) {
        List<String> paragraphs = new ArrayList<>();
        for (String block : pageText.split(PARAGRAPH_END)) {
            StringBuilder joined = new StringBuilder();
            for (String line : block.split("\\R")) {
                String trimmed = line.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (joined.length() > 0) {
                    joined.append(' ');
                }
                joined.append(trimmed);
            }
            if (joined.length() > 0) {
                paragraphs.add(joined.toString());
            }
        }
        return paragraphs;
    }
}
