package scripta.software.core.model.api.security;

import lombok.Data;
import lombok.EqualsAndHashCode;

import scripta.software.common.model.api.PDFFile;

@Data
@EqualsAndHashCode(callSuper = true)
public class PDFVerificationRequest extends PDFFile {}
