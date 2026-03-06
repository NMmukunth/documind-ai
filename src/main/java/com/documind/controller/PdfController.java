package com.documind.controller;

import com.documind.rag.RagService;
import com.documind.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private RagService ragService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadPdf(
            @RequestParam("file") MultipartFile file) {
        try {
            String extractedText = pdfService.extractText(file);
            ragService.loadDocument(extractedText);

            return ResponseEntity.ok(Map.of(
                "message", "PDF uploaded and processed successfully! You can now ask questions."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to process PDF. Please try again."));
        }
    }
}
