package com.tailorcv.resume.service;

import com.tailorcv.resume.exception.ApiException;
import java.io.IOException;
import java.util.Arrays;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfExtractionService {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final byte[] PDF_HEADER = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    public ExtractedPdf extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a PDF file to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF must be 5 MB or smaller.");
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only text-based PDF files are supported.");
        }
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Could not read the PDF file.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF must be 5 MB or smaller.");
        }
        if (bytes.length < PDF_HEADER.length || !Arrays.equals(Arrays.copyOf(bytes, PDF_HEADER.length), PDF_HEADER)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The uploaded file is not a PDF.");
        }

        try (PDDocument document = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(document).trim();
            if (text.isBlank()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "No selectable text found. Upload a text-based PDF; scanned PDFs need OCR, which is not supported.");
            }
            String suppliedName = file.getOriginalFilename();
            String filename = suppliedName == null ? "resume.pdf" : suppliedName.replace('\\', '/');
            filename = filename.substring(filename.lastIndexOf('/') + 1);
            if (filename.isBlank()) filename = "resume.pdf";
            if (filename.length() > 255) filename = filename.substring(filename.length() - 255);
            return new ExtractedPdf(filename, bytes, text);
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not extract text. The PDF may be damaged or password-protected.");
        }
    }

    public record ExtractedPdf(String filename, byte[] bytes, String text) {}
}
