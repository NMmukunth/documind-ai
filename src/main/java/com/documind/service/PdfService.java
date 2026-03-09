package com.documind.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfService {

    @Autowired
    private ImageAnalysisService imageAnalysisService;

    public String extractText(
            MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                "No file uploaded");
        }
        if (!file.getOriginalFilename()
                .toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException(
                "Only PDF files accepted");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException(
                "File too large. Max 10MB");
        }

        try (PDDocument document = 
                PDDocument.load(file.getInputStream())) {

            if (document.isEncrypted()) {
                throw new IllegalArgumentException(
                    "Cannot process encrypted PDFs");
            }

            // Step 1 — Extract normal text
            PDFTextStripper stripper = 
                new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            System.out.println("Text extracted: " 
                + text.length() + " characters");

            // Step 2 — Extract and analyze images
            System.out.println(
                "Checking for images in PDF...");
            String imageText = imageAnalysisService
                .extractImagesText(document);

            if (!imageText.isEmpty()) {
                System.out.println(
                    "Image content found and analyzed!");
                // Combine text + image descriptions
                text = text + "\n\n" 
                    + "=== IMAGE CONTENT ===\n" 
                    + imageText;
            } else {
                System.out.println(
                    "No significant images found.");
            }

            if (text.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "No content found in PDF");
            }

            System.out.println("Total content: " 
                + text.length() + " characters");
            return text;
        }
    }
}