package scripta.software.core.model.api.security;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import scripta.software.common.model.api.PDFFile;

@Data
@EqualsAndHashCode(callSuper = true)
public class PDFPasswordRequest extends PDFFile {

    @Schema(
            description = "The password of the PDF file",
            format = "password",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String password;
}
