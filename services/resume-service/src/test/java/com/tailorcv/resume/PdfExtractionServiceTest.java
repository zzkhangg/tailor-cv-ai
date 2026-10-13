package com.tailorcv.resume;

import com.tailorcv.resume.exception.ApiException;
import com.tailorcv.resume.service.PdfExtractionService;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class PdfExtractionServiceTest {
    private final PdfExtractionService service = new PdfExtractionService();

    static byte[] pdf(String text) throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            if (text != null) {
                try (var content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    @Test void extractsTextPdf() throws Exception {
        var file = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdf("Jane Doe"));
        assertEquals("Jane Doe", service.extract(file).text());
    }

    @Test void rejectsScannedPdfWithoutSelectableText() throws Exception {
        var file = new MockMultipartFile("file", "scan.pdf", "application/pdf", pdf(null));
        var error = assertThrows(ApiException.class, () -> service.extract(file));
        assertEquals(422, error.status().value());
        assertTrue(error.getMessage().contains("scanned"));
    }

    @Test void rejectsWrongMimeAndOversize() {
        var wrongMime = new MockMultipartFile("file", "resume.pdf", "text/plain", "%PDF-fake".getBytes());
        assertEquals(415, assertThrows(ApiException.class, () -> service.extract(wrongMime)).status().value());
        var tooBig = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[(int) PdfExtractionService.MAX_BYTES + 1]);
        assertEquals(413, assertThrows(ApiException.class, () -> service.extract(tooBig)).status().value());
    }
}
