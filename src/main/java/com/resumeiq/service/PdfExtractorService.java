package com.resumeiq.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Extracts plain text from a PDF resume using Apache PDFBox 3.x
 * NOTE: PDFBox 3.x removed PDDocument.load() — use Loader.loadPDF() instead.
 */
@Service
public class PdfExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractorService.class);
    private static final int MAX_PAGES = 10;

    public String extractText(MultipartFile file) throws IOException {
        validatePdf(file);

        // PDFBox 3.x: use Loader.loadPDF(byte[]) not PDDocument.load()
        byte[] bytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {

            if (document.isEncrypted()) {
                throw new IllegalArgumentException(
                    "The PDF is password-protected. Please upload an unlocked PDF.");
            }

            int pages = document.getNumberOfPages();
            if (pages > MAX_PAGES) {
                throw new IllegalArgumentException(
                    "PDF has " + pages + " pages. Maximum allowed is " + MAX_PAGES + ".");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String rawText = stripper.getText(document);

            String cleaned = cleanText(rawText);
            log.debug("Extracted {} chars from PDF ({} pages)", cleaned.length(), pages);

            if (cleaned.isBlank()) {
                throw new IllegalArgumentException(
                    "Could not extract text from this PDF. " +
                    "It may be a scanned image. Please upload a text-based PDF.");
            }

            return cleaned;
        }
    }

    public String extractTextFromBytes(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return cleanText(stripper.getText(document));
        }
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a PDF file.");
        }
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        if (!"application/pdf".equals(contentType) && !fileName.endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are accepted.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB limit.");
        }
    }

    private String cleanText(String raw) {
        if (raw == null) return "";
        return raw
            .replaceAll("\\r\\n", "\n")
            .replaceAll("[ \\t]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }
}