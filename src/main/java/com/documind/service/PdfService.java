package com.documind.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfService {

    public String extractText(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are accepted");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large. Maximum is 10MB");
        }

        // PDFBox 2.x API — uses PDDocument.load(InputStream)
        try (PDDocument document = PDDocument.load(file.getInputStream())) {

            if (document.isEncrypted()) {
                throw new IllegalArgumentException(
                    "Cannot process encrypted/password-protected PDFs");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(true);
            stripper.setWordSeparator(" ");
            stripper.setLineSeparator("\n");
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "No text found. This PDF may contain only scanned images.");
            }

            System.out.println("PDF extracted: " + text.length() +
                " characters, " + document.getNumberOfPages() + " pages");
            return text;
        }
    }
}