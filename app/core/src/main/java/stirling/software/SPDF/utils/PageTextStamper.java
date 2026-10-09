package stirling.software.SPDF.utils;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.springframework.core.io.ClassPathResource;

import lombok.extern.slf4j.Slf4j;

/**
 * Draws one line of text at one of nine anchor positions (1 = top-left ... 9 = bottom-right) as a
 * reader sees the page: positions are measured inside the crop box and follow the page's /Rotate,
 * so the text lands in the visible corner and reads upright on scanned, rotated or cropped pages.
 *
 * <p>Text the chosen Standard 14 font cannot encode (anything outside WinAnsi, such as Cyrillic,
 * Greek or CJK) is drawn with a bundled Unicode font, embedded as a subset. Characters no bundled
 * font covers are dropped rather than failing the whole document.
 *
 * <p>Fonts are cached per instance, so use one instance per document. Not thread-safe.
 */
@Slf4j
public class PageTextStamper {

    private static final String FONT_DIR = "static/fonts/";
    private static final String DEFAULT_UNICODE_FONT = "NotoSans-Regular.ttf";

    private final PDDocument document;
    private final PDFont standardFont;
    private final String unicodeStyleFont;
    private final float fontSize;
    private final Color color;
    private final float marginFactor;
    private final Map<String, PDFont> loadedFonts = new HashMap<>();

    /**
     * @param fontType "helvetica", "times" or "courier" (case-insensitive); anything else is
     *     Helvetica
     * @param marginFactor distance from the page edge as a fraction of the visible page size
     */
    public PageTextStamper(
            PDDocument document, String fontType, float fontSize, Color color, float marginFactor) {
        this.document = document;
        this.fontSize = fontSize;
        this.color = color == null ? Color.BLACK : color;
        this.marginFactor = marginFactor;
        String type = fontType == null ? "" : fontType.toLowerCase(Locale.ROOT);
        switch (type) {
            case "times" -> {
                standardFont = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
                unicodeStyleFont = "DejaVuSerif.ttf";
            }
            case "courier" -> {
                standardFont = new PDType1Font(Standard14Fonts.FontName.COURIER);
                unicodeStyleFont = "DejaVuSansMono.ttf";
            }
            default -> {
                standardFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                unicodeStyleFont = DEFAULT_UNICODE_FONT;
            }
        }
    }

    /** Margin as a fraction of the page: small/medium/large/x-large; anything else is medium. */
    public static float marginFactor(String customMargin) {
        return switch (customMargin == null ? "" : customMargin.toLowerCase(Locale.ROOT)) {
            case "small" -> 0.02f;
            case "large" -> 0.05f;
            case "x-large" -> 0.075f;
            default -> 0.035f;
        };
    }

    /** Parses a hex colour such as "#FF0000"; blank or invalid input is black. */
    public static Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) {
            return Color.BLACK;
        }
        try {
            return Color.decode(hex.trim());
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }

    /**
     * Draws {@code rawText} on {@code page}. Line breaks and other control characters become
     * spaces; blank text draws nothing.
     *
     * @param position 1-9, clamped into range
     */
    public void stamp(PDPage page, int position, String rawText) throws IOException {
        String text = sanitize(rawText);
        if (text.isBlank()) {
            return;
        }
        PDFont font = fontFor(text);
        if (!canEncode(font, text)) {
            text = keepEncodable(font, text);
            if (text.isBlank()) {
                return;
            }
        }

        int pos = Math.max(1, Math.min(9, position));
        int col = ((pos - 1) % 3) + 1; // 1 = left, 2 = centre, 3 = right
        int row = ((pos - 1) / 3) + 1; // 1 = top, 2 = middle, 3 = bottom

        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        float ascent = fontSize * 0.75f;
        float descent = -fontSize * 0.25f;
        PDFontDescriptor descriptor = font.getFontDescriptor();
        if (descriptor != null && descriptor.getAscent() != 0) {
            ascent = descriptor.getAscent() / 1000f * fontSize;
            descent = descriptor.getDescent() / 1000f * fontSize;
        }

        PDRectangle box = page.getCropBox();
        int rotation = ((page.getRotation() % 360) + 360) % 360;
        boolean quarterTurn = rotation == 90 || rotation == 270;
        float visualWidth = quarterTurn ? box.getHeight() : box.getWidth();
        float visualHeight = quarterTurn ? box.getWidth() : box.getHeight();
        float marginX = marginFactor * visualWidth;
        float marginY = marginFactor * visualHeight;

        // Baseline origin in the reader's frame: origin at the visible bottom-left corner.
        float vx =
                switch (col) {
                    case 1 -> marginX;
                    case 2 -> (visualWidth - textWidth) / 2f;
                    default -> visualWidth - marginX - textWidth;
                };
        float vy =
                switch (row) {
                    case 1 -> visualHeight - marginY - ascent;
                    case 2 -> (visualHeight - (ascent + descent)) / 2f;
                    default -> marginY;
                };

        try (PDPageContentStream stream =
                new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(color);
            stream.setTextMatrix(toUserSpace(box, rotation, vx, vy));
            stream.showText(text);
            stream.endText();
        }
    }

    /**
     * Maps a baseline origin in the reader's frame to a text matrix in the page's user space. A
     * viewer turns the page clockwise by /Rotate, so the text is turned the same angle
     * counter-clockwise to read upright.
     */
    static Matrix toUserSpace(PDRectangle box, int rotation, float vx, float vy) {
        float llx = box.getLowerLeftX();
        float lly = box.getLowerLeftY();
        float w = box.getWidth();
        float h = box.getHeight();
        return switch (rotation) {
            case 90 -> Matrix.getRotateInstance(Math.PI / 2, llx + w - vy, lly + vx);
            case 180 -> Matrix.getRotateInstance(Math.PI, llx + w - vx, lly + h - vy);
            case 270 -> Matrix.getRotateInstance(3 * Math.PI / 2, llx + vy, lly + h - vx);
            default -> Matrix.getTranslateInstance(llx + vx, lly + vy);
        };
    }

    private PDFont fontFor(String text) {
        if (canEncode(standardFont, text)) {
            return standardFont;
        }
        PDFont styled = loadBundled(unicodeStyleFont);
        if (styled != null && canEncode(styled, text)) {
            return styled;
        }
        PDFont reference = styled != null ? styled : standardFont;
        PDFont script = loadBundled(scriptFontFor(firstUnencodable(reference, text)));
        if (script != null && canEncode(script, text)) {
            return script;
        }
        if (script != null) {
            return script;
        }
        return reference;
    }

    private PDFont loadBundled(String fileName) {
        if (loadedFonts.containsKey(fileName)) {
            return loadedFonts.get(fileName);
        }
        PDFont font = null;
        ClassPathResource resource = new ClassPathResource(FONT_DIR + fileName);
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                font = PDType0Font.load(document, in, true);
            } catch (IOException e) {
                log.warn("Could not load bundled font {}: {}", fileName, e.getMessage());
            }
        } else {
            log.warn("Bundled font {} is missing from the classpath", fileName);
        }
        loadedFonts.put(fileName, font);
        return font;
    }

    static String scriptFontFor(int codePoint) {
        if (codePoint < 0) {
            return DEFAULT_UNICODE_FONT;
        }
        return switch (Character.UnicodeScript.of(codePoint)) {
            case HAN, BOPOMOFO -> "NotoSansSC-Regular.ttf";
            case HIRAGANA, KATAKANA -> "NotoSansJP-Regular.ttf";
            case HANGUL -> "NotoSansKR-Regular.ttf";
            case ARABIC -> "NotoSansArabic-Regular.ttf";
            case HEBREW -> "NotoSansHebrew-Regular.ttf";
            case THAI -> "NotoSansThai-Regular.ttf";
            case DEVANAGARI -> "NotoSansDevanagari-Regular.ttf";
            case MALAYALAM -> "NotoSansMalayalam-Regular.ttf";
            case TIBETAN -> "NotoSerifTibetan-Regular.ttf";
            default -> DEFAULT_UNICODE_FONT;
        };
    }

    private static int firstUnencodable(PDFont font, String text) {
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            if (!canEncode(font, new String(Character.toChars(cp)))) {
                return cp;
            }
            i += Character.charCount(cp);
        }
        return -1;
    }

    private static String keepEncodable(PDFont font, String text) {
        StringBuilder kept = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            if (canEncode(font, ch)) {
                kept.append(ch);
            }
            i += Character.charCount(cp);
        }
        return kept.toString();
    }

    static boolean canEncode(PDFont font, String text) {
        if (font == null) {
            return false;
        }
        try {
            font.encode(text);
            return true;
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException e) {
            return false;
        }
    }

    static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            out.appendCodePoint(Character.isISOControl(cp) ? ' ' : cp);
            i += Character.charCount(cp);
        }
        return out.toString();
    }
}
