package scripta.software.core.Factories;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import scripta.software.common.model.api.misc.HighContrastColorCombination;
import scripta.software.common.model.api.misc.ReplaceAndInvert;
import scripta.software.common.util.TempFileManager;
import scripta.software.common.util.misc.ColorSpaceConversionStrategy;
import scripta.software.common.util.misc.CustomColorReplaceStrategy;
import scripta.software.common.util.misc.InvertFullColorStrategy;
import scripta.software.common.util.misc.ReplaceAndInvertColorStrategy;
import scripta.software.core.config.EndpointConfiguration;

@Component
@RequiredArgsConstructor
public class ReplaceAndInvertColorFactory {

    private final TempFileManager tempFileManager;
    private final EndpointConfiguration endpointConfiguration;

    public ReplaceAndInvertColorStrategy replaceAndInvert(
            MultipartFile file,
            ReplaceAndInvert replaceAndInvertOption,
            HighContrastColorCombination highContrastColorCombination,
            String backGroundColor,
            String textColor) {

        if (replaceAndInvertOption == null) {
            return null;
        }

        // Check Ghostscript availability for CMYK conversion
        if (replaceAndInvertOption == ReplaceAndInvert.COLOR_SPACE_CONVERSION
                && !endpointConfiguration.isGroupEnabled("Ghostscript")) {
            throw new IllegalStateException(
                    "CMYK color space conversion requires Ghostscript, which is not available on this system");
        }

        return switch (replaceAndInvertOption) {
            case CUSTOM_COLOR, HIGH_CONTRAST_COLOR ->
                    new CustomColorReplaceStrategy(
                            file,
                            replaceAndInvertOption,
                            textColor,
                            backGroundColor,
                            highContrastColorCombination);
            case FULL_INVERSION -> new InvertFullColorStrategy(file, replaceAndInvertOption);
            case COLOR_SPACE_CONVERSION ->
                    new ColorSpaceConversionStrategy(file, replaceAndInvertOption, tempFileManager);
        };
    }
}
