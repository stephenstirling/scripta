package stirling.software.SPDF.controller.api.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import stirling.software.SPDF.model.api.misc.AddBatesNumbersRequest;
import stirling.software.common.service.CustomPDFDocumentFactory;
import stirling.software.common.util.TempFile;
import stirling.software.common.util.TempFileManager;

@ExtendWith(MockitoExtension.class)
class BatesNumberingControllerTest {

    @Mock private CustomPDFDocumentFactory pdfDocumentFactory;
    @Mock private TempFileManager tempFileManager;

    @InjectMocks private BatesNumberingController controller;

    @BeforeEach
    void setUp() throws Exception {
        lenient()
                .when(tempFileManager.createManagedTempFile(anyString()))
                .thenAnswer(
                        inv -> {
                            File f =
                                    Files.createTempFile("bates-test", inv.<String>getArgument(0))
                                            .toFile();
                            f.deleteOnExit();
                            TempFile tf = mock(TempFile.class);
                            lenient().when(tf.getFile()).thenReturn(f);
                            lenient().when(tf.getPath()).thenReturn(f.toPath());
                            return tf;
                        });
        lenient()
                .when(pdfDocumentFactory.load(any(MultipartFile.class)))
                .thenAnswer(inv -> Loader.loadPDF(inv.<MultipartFile>getArgument(0).getBytes()));
    }

    private static MockMultipartFile pdf(String name, int pages) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage(PDRectangle.LETTER));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return new MockMultipartFile(
                    "fileInput", name, MediaType.APPLICATION_PDF_VALUE, out.toByteArray());
        }
    }

    private static AddBatesNumbersRequest request(MockMultipartFile... files) {
        AddBatesNumbersRequest request = new AddBatesNumbersRequest();
        request.setFileInput(files);
        request.setPrefix("ACME");
        return request;
    }

    private static byte[] body(ResponseEntity<Resource> response) throws IOException {
        try (InputStream in = response.getBody().getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries.put(entry.getName(), in.readAllBytes());
            }
        }
        return entries;
    }

    private static List<String> pageTexts(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            java.util.ArrayList<String> texts = new java.util.ArrayList<>();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                texts.add(stripper.getText(doc).trim());
            }
            return texts;
        }
    }

    @Test
    @DisplayName("Numbering continues from one file to the next, in the order sent")
    void numberingContinuesAcrossFiles() throws Exception {
        ResponseEntity<Resource> response =
                controller.addBatesNumbers(request(pdf("a.pdf", 3), pdf("b.pdf", 2)));

        Map<String, byte[]> entries = unzip(body(response));
        assertThat(entries.keySet()).containsExactly("a_bates.pdf", "b_bates.pdf");
        assertThat(pageTexts(entries.get("a_bates.pdf")))
                .containsExactly("ACME000001", "ACME000002", "ACME000003");
        assertThat(pageTexts(entries.get("b_bates.pdf")))
                .containsExactly("ACME000004", "ACME000005");
    }

    @Test
    @DisplayName("A single file comes back as a PDF, not a ZIP")
    void singleFileReturnsPdf() throws Exception {
        AddBatesNumbersRequest request = request(pdf("only.pdf", 2));
        request.setStartNumber(41);
        request.setDigits(4);
        request.setSuffix("-C");

        ResponseEntity<Resource> response = controller.addBatesNumbers(request);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(pageTexts(body(response))).containsExactly("ACME0041-C", "ACME0042-C");
    }

    @Test
    @DisplayName("Files can be named after their Bates range")
    void nameByRange() throws Exception {
        AddBatesNumbersRequest request = request(pdf("x.pdf", 2), pdf("y.pdf", 1));
        request.setNameByRange(true);

        Map<String, byte[]> entries = unzip(body(controller.addBatesNumbers(request)));

        assertThat(entries.keySet())
                .containsExactly("ACME000001-ACME000002.pdf", "ACME000003.pdf");
    }

    @Test
    @DisplayName("Duplicate input names get distinct entries")
    void duplicateNamesAreDisambiguated() throws Exception {
        Map<String, byte[]> entries =
                unzip(
                        body(
                                controller.addBatesNumbers(
                                        request(pdf("same.pdf", 1), pdf("same.pdf", 1)))));

        assertThat(entries.keySet()).containsExactly("same_bates.pdf", "same_bates (2).pdf");
    }

    @Test
    @DisplayName("Digits are clamped to 1..15")
    void digitsAreClamped() {
        AddBatesNumbersRequest request = new AddBatesNumbersRequest();
        assertThat(BatesNumberingController.batesLabel(request, 1, 7)).isEqualTo("7");
        assertThat(BatesNumberingController.batesLabel(request, 3, 7)).isEqualTo("007");
    }

    @Test
    @DisplayName("No files or a negative start is rejected")
    void rejectsBadInput() throws Exception {
        AddBatesNumbersRequest empty = new AddBatesNumbersRequest();
        assertThatThrownBy(() -> controller.addBatesNumbers(empty))
                .isInstanceOf(IllegalArgumentException.class);

        AddBatesNumbersRequest negative = request(pdf("a.pdf", 1));
        negative.setStartNumber(-1);
        assertThatThrownBy(() -> controller.addBatesNumbers(negative))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
