package stirling.software.SPDF.controller.api.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;

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

import stirling.software.SPDF.model.api.misc.AddHeaderFooterRequest;
import stirling.software.common.service.CustomPDFDocumentFactory;
import stirling.software.common.util.TempFile;
import stirling.software.common.util.TempFileManager;

@ExtendWith(MockitoExtension.class)
class HeaderFooterControllerTest {

    @Mock private CustomPDFDocumentFactory pdfDocumentFactory;
    @Mock private TempFileManager tempFileManager;

    @InjectMocks private HeaderFooterController controller;

    @BeforeEach
    void setUp() throws Exception {
        lenient()
                .when(tempFileManager.createManagedTempFile(anyString()))
                .thenAnswer(
                        inv -> {
                            File f =
                                    Files.createTempFile("hf-test", inv.<String>getArgument(0))
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
                doc.addPage(new PDPage(PDRectangle.A4));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return new MockMultipartFile(
                    "fileInput", name, MediaType.APPLICATION_PDF_VALUE, out.toByteArray());
        }
    }

    private static String pageText(ResponseEntity<Resource> response, int page)
            throws IOException {
        try (InputStream in = response.getBody().getInputStream();
                PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            return stripper.getText(doc);
        }
    }

    @Test
    @DisplayName("Tokens are filled in for every slot on every page")
    void fillsTokens() throws Exception {
        AddHeaderFooterRequest request = new AddHeaderFooterRequest();
        request.setFileInput(pdf("contract.pdf", 2));
        request.setHeaderLeft("{filename}");
        request.setHeaderRight("CONFIDENTIAL");
        request.setFooterCenter("Page {n} of {total}");

        ResponseEntity<Resource> response = controller.addHeaderFooter(request);

        String second = pageText(response, 2);
        assertThat(second).contains("contract", "CONFIDENTIAL", "Page 2 of 2");
    }

    @Test
    @DisplayName("Only the selected pages are stamped, numbered from the starting number")
    void respectsPageSelection() throws Exception {
        AddHeaderFooterRequest request = new AddHeaderFooterRequest();
        request.setFileInput(pdf("doc.pdf", 3));
        request.setFooterRight("{n}");
        request.setPagesToApply("2-3");
        request.setStartingNumber(10);

        ResponseEntity<Resource> first = controller.addHeaderFooter(request);
        assertThat(pageText(first, 1).trim()).isEmpty();

        request.setFileInput(pdf("doc.pdf", 3));
        ResponseEntity<Resource> again = controller.addHeaderFooter(request);
        assertThat(pageText(again, 3).trim()).isEqualTo("12");
    }

    @Test
    @DisplayName("{date} uses the pattern, and a bad pattern falls back to ISO")
    void dateToken() {
        LocalDate today = LocalDate.now();
        assertThat(HeaderFooterController.formatToday("yyyy")).isEqualTo(
                String.valueOf(today.getYear()));
        assertThat(HeaderFooterController.formatToday("not a {pattern")).isEqualTo(
                today.toString());
        assertThat(HeaderFooterController.formatToday(null)).isEqualTo(today.toString());
    }

    @Test
    @DisplayName("Requests with every slot empty are rejected")
    void rejectsEmptySlots() throws Exception {
        AddHeaderFooterRequest request = new AddHeaderFooterRequest();
        request.setFileInput(pdf("doc.pdf", 1));
        request.setFooterLeft("   ");

        assertThatThrownBy(() -> controller.addHeaderFooter(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
