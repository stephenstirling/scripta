package stirling.software.SPDF.model.api.misc;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

import lombok.Data;
import lombok.EqualsAndHashCode;

import stirling.software.SPDF.model.api.MultiplePDFFiles;

@Data
@EqualsAndHashCode(callSuper = true)
public class AddBatesNumbersRequest extends MultiplePDFFiles {

    @Schema(
            description = "Text placed before the number (e.g. 'ACME')",
            example = "ACME",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private String prefix;

    @Schema(
            description = "Text placed after the number (e.g. '-CONFIDENTIAL')",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private String suffix;

    @Schema(
            description =
                    "Bates number of the first page of the first file. Numbering continues"
                            + " across the files in the order they are sent.",
            minimum = "0",
            defaultValue = "1",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private long startNumber = 1;

    @Schema(
            description = "Minimum number of digits; shorter numbers are zero-padded",
            minimum = "1",
            maximum = "15",
            defaultValue = "6",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private int digits = 6;

    @Schema(
            description =
                    "Position: 1-9 (1=top-left, 2=top-center, 3=top-right, 4=middle-left,"
                            + " 5=middle-center, 6=middle-right, 7=bottom-left, 8=bottom-center,"
                            + " 9=bottom-right)",
            type = "integer",
            allowableValues = {"1", "2", "3", "4", "5", "6", "7", "8", "9"},
            defaultValue = "9",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private int position = 9;

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

    @Schema(
            description =
                    "Name each output file after its Bates range (e.g. ACME000001-ACME000042.pdf)"
                            + " instead of the original file name",
            defaultValue = "false",
            requiredMode = RequiredMode.NOT_REQUIRED)
    private boolean nameByRange;
}
