package com.documind.controller;

import com.documind.service.ChatService;
import com.documind.util.PdfUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private PdfUtil pdfUtil;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(
            @RequestBody Map<String, String> body) {
        try {
            String question = body.get("question");
            String answer = chatService.askQuestion(question);
            return ResponseEntity.ok(Map.of("answer", answer));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get answer: " + e.getMessage()));
        }
    }

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarize() {
        try {
            String summary = chatService.summarizeDocument();
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to summarize: " + e.getMessage()));
        }
    }

    @PostMapping("/download-summary")
    public ResponseEntity<byte[]> downloadSummary(
            @RequestBody Map<String, String> body) {
        try {
            String summaryText = body.get("summary");
            if (summaryText == null || summaryText.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            byte[] pdfBytes = pdfUtil.generateSummaryPdf(summaryText);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "documind-summary.pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
