package stirling.software.SPDF.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PageTextStamperTest {

    /** Collects every glyph position on the first page. */
    private static final class PositionCollector extends PDFTextStripper {
        final List<TextPosition> positions = new ArrayList<>();

        PositionCollector() throws IOException {
            setStartPage(1);
            setEndPage(1);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) {
            positions.addAll(textPositions);
        }
    }

    private static PDDocument roundTrip(PDDocument doc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        doc.close();
        return Loader.loadPDF(out.toByteArray());
    }

    private static PDDocument stampedPage(int rotation, int position, String text, String font)
            throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        page.setRotation(rotation);
        doc.addPage(page);
        new PageTextStamper(doc, font, 12f, Color.BLACK, 0.035f).stamp(page, position, text);
        return roundTrip(doc);
    }

    @ParameterizedTest(name = "rotation {0}")
    @ValueSource(ints = {0, 90, 180, 270})
    @DisplayName("Bottom-right text lands in the visible bottom-right corner and reads upright")
    void bottomRightFollowsRotation(int rotation) throws IOException {
        try (PDDocument doc = stampedPage(rotation, 9, "X", "helvetica")) {
            PositionCollector collector = new PositionCollector();
            collector.getText(doc);
            assertThat(collector.positions).isNotEmpty();
            TextPosition glyph = collector.positions.getFirst();

            boolean quarterTurn = rotation == 90 || rotation == 270;
            float visibleWidth = quarterTurn ? 792 : 612;
            float visibleHeight = quarterTurn ? 612 : 792;
            // getX/getY are measured from the visible top-left corner.
            assertThat(glyph.getX()).isGreaterThan(visibleWidth * 0.8f);
            assertThat(glyph.getY()).isGreaterThan(visibleHeight * 0.8f);
            // Text turned with the page reads upright once the viewer applies /Rotate.
            assertThat((int) glyph.getDir()).isEqualTo(rotation);
        }
    }

    @ParameterizedTest(name = "rotation {0}")
    @ValueSource(ints = {0, 90, 180, 270})
    @DisplayName("Top-left text lands in the visible top-left corner")
    void topLeftFollowsRotation(int rotation) throws IOException {
        try (PDDocument doc = stampedPage(rotation, 1, "X", "helvetica")) {
            PositionCollector collector = new PositionCollector();
            collector.getText(doc);
            TextPosition glyph = collector.positions.getFirst();

            boolean quarterTurn = rotation == 90 || rotation == 270;
            float visibleWidth = quarterTurn ? 792 : 612;
            float visibleHeight = quarterTurn ? 612 : 792;
            assertThat(glyph.getX()).isLessThan(visibleWidth * 0.2f);
            assertThat(glyph.getY()).isLessThan(visibleHeight * 0.2f);
        }
    }

    @Test
    @DisplayName("The text matrix is offset by the crop box origin")
    void cropBoxOffsetIsApplied() {
        PDRectangle crop = new PDRectangle(100, 50, 300, 400);
        Matrix upright = PageTextStamper.toUserSpace(crop, 0, 10, 20);
        assertThat(upright.getTranslateX()).isCloseTo(110f, within(0.01f));
        assertThat(upright.getTranslateY()).isCloseTo(70f, within(0.01f));

        // 90°: the reader's x runs up the page's y axis; the reader's y runs right to left.
        Matrix quarter = PageTextStamper.toUserSpace(crop, 90, 10, 20);
        assertThat(quarter.getTranslateX()).isCloseTo(100 + 300 - 20f, within(0.01f));
        assertThat(quarter.getTranslateY()).isCloseTo(50 + 10f, within(0.01f));
    }

    @Test
    @DisplayName("Cyrillic text falls back to an embedded Unicode font instead of failing")
    void cyrillicTextIsEmbedded() throws IOException {
        try (PDDocument doc = stampedPage(0, 8, "Сторінка 1", "helvetica")) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Сторінка 1");
        }
    }

    @Test
    @DisplayName("Chinese text uses the CJK fallback font")
    void chineseTextIsEmbedded() throws IOException {
        try (PDDocument doc = stampedPage(0, 8, "第1页", "times")) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("第1页");
        }
    }

    @Test
    @DisplayName("Latin text keeps the Standard 14 font")
    void latinTextUsesStandardFont() throws IOException {
        try (PDDocument doc = stampedPage(0, 8, "Page 1", "courier")) {
            PositionCollector collector = new PositionCollector();
            collector.getText(doc);
            assertThat(collector.positions.getFirst().getFont().getName()).contains("Courier");
        }
    }

    @Test
    @DisplayName("Control characters become spaces and blank text draws nothing")
    void sanitizesText() throws IOException {
        assertThat(PageTextStamper.sanitize("a\nb\tc")).isEqualTo("a b c");
        try (PDDocument doc = stampedPage(0, 8, "   ", "helvetica")) {
            assertThat(new PDFTextStripper().getText(doc).trim()).isEmpty();
        }
    }

    @Test
    @DisplayName("Script detection picks the matching bundled font")
    void scriptFontSelection() {
        assertThat(PageTextStamper.scriptFontFor("한".codePointAt(0)))
                .isEqualTo("NotoSansKR-Regular.ttf");
        assertThat(PageTextStamper.scriptFontFor("ש".codePointAt(0)))
                .isEqualTo("NotoSansHebrew-Regular.ttf");
        assertThat(PageTextStamper.scriptFontFor("Ж".codePointAt(0)))
                .isEqualTo("NotoSans-Regular.ttf");
    }

    @Test
    @DisplayName("Margins and colours parse with safe fallbacks")
    void helpers() {
        assertThat(PageTextStamper.marginFactor("small")).isEqualTo(0.02f);
        assertThat(PageTextStamper.marginFactor(null)).isEqualTo(0.035f);
        assertThat(PageTextStamper.parseColor("#FF0000")).isEqualTo(Color.RED);
        assertThat(PageTextStamper.parseColor("nope")).isEqualTo(Color.BLACK);
    }
}
