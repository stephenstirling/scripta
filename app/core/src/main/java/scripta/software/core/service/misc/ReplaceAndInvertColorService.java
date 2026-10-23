package scripta.software.core.service.misc;

import java.io.IOException;

import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import scripta.software.common.model.api.misc.HighContrastColorCombination;
import scripta.software.common.model.api.misc.ReplaceAndInvert;
import scripta.software.common.util.misc.ReplaceAndInvertColorStrategy;
import scripta.software.core.Factories.ReplaceAndInvertColorFactory;

@Service
@RequiredArgsConstructor
public class ReplaceAndInvertColorService {
    private final ReplaceAndInvertColorFactory replaceAndInvertColorFactory;

    public InputStreamResource replaceAndInvertColor(
            MultipartFile file,
            ReplaceAndInvert replaceAndInvertOption,
            HighContrastColorCombination highContrastColorCombination,
            String backGroundColor,
            String textColor)
            throws IOException {

        ReplaceAndInvertColorStrategy replaceColorStrategy =
                replaceAndInvertColorFactory.replaceAndInvert(
                        file,
                        replaceAndInvertOption,
                        highContrastColorCombination,
                        backGroundColor,
                        textColor);

        return replaceColorStrategy.replace();
    }
}
