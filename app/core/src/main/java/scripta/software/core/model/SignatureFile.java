package scripta.software.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureFile {
    private String fileName;
    private String category; // "Personal" or "Shared"
}
