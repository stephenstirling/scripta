package stirling.software.SPDF.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OcrDocxExporterTest {

    @Test
    void joinsWrappedLinesAndSplitsOnParagraphEnds() {
        List<String> paragraphs =
                OcrDocxExporter.toParagraphs("first line\nsecond line \nnext paragraph ");

        assertEquals(List.of("first line second line", "next paragraph"), paragraphs);
    }

    @Test
    void skipsBlankBlocks() {
        assertEquals(List.of(), OcrDocxExporter.toParagraphs("\n   \n"));
    }

    @Test
    void writesTheTextLayerToADocx(@TempDir Path dir) throws IOException {
        Path pdf = dir.resolve("in.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 700);
                content.showText("Scanned invoice 42");
                content.endText();
            }
            document.save(pdf.toFile());
        }
        Path docx = dir.resolve("out.docx");

        OcrDocxExporter.export(pdf, docx);

        try (InputStream in = Files.newInputStream(docx);
                XWPFDocument word = new XWPFDocument(in);
                XWPFWordExtractor extractor = new XWPFWordExtractor(word)) {
            assertTrue(extractor.getText().contains("Scanned invoice 42"));
        }
    }
}
