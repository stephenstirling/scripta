package scripta.software.core.model.api.misc;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

import lombok.Data;
import lombok.EqualsAndHashCode;

import scripta.software.common.model.api.PDFFile;

@Data
@EqualsAndHashCode(callSuper = true)
public class AddHeaderFooterRequest extends PDFFile {

    private static final String TOKENS =
            " Tokens: {n}=page number, {total}=total pages, {filename}=file name without"
                    + " extension, {date}=today's date.";

    @Schema(description = "Text for the top-left header slot." + TOKENS)
    private String headerLeft;

    @Schema(description = "Text for the top-centre header slot." + TOKENS)
    private String headerCenter;

    @Schema(description = "Text for the top-right header slot." + TOKENS)
    private String headerRight;

    @Schema(description = "Text for the bottom-left footer slot." + TOKENS)
    private String footerLeft;

    @Schema(description = "Text for the bottom-centre footer slot." + TOKENS)
    private String footerCenter;

    @Schema(description = "Text for the bottom-right footer slot." + TOKENS)
    private String footerRight;

    @Schema(
            description = "Which pages get the header and footer (e.g. '1,3-5,7' or 'all')",
            defaultValue = "all",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private String pagesToApply;

    @Schema(
            description = "Number used for {n} on the first page of the document",
            minimum = "0",
            defaultValue = "1",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private int startingNumber = 1;

    @Schema(
            description = "Pattern for {date}, in java.time DateTimeFormatter syntax",
            example = "yyyy-MM-dd",
            defaultValue = "yyyy-MM-dd",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private String dateFormat;

    @Schema(
            description = "Custom margin: small/medium/large/x-large",
            allowableValues = {"small", "medium", "large", "x-large"},
            defaultValue = "medium",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private String customMargin;

    @Schema(
            description = "Font size in points",
            minimum = "1",
            defaultValue = "10",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private float fontSize = 10f;

    @Schema(
            description = "Font family",
            allowableValues = {"helvetica", "courier", "times"},
            defaultValue = "helvetica",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private String fontType;

    @Schema(
            description = "Hex colour (e.g. #000000)",
            example = "#000000",
            defaultValue = "#000000",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private String fontColor;
}
