package scripta.software.core.model.api.misc;

import lombok.Data;
import lombok.EqualsAndHashCode;

import scripta.software.common.model.api.PDFFile;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExtractAttachmentsRequest extends PDFFile {}
