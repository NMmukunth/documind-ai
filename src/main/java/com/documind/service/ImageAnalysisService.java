package com.documind.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class ImageAnalysisService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractImagesText(PDDocument document) {
        StringBuilder imageDescriptions = new StringBuilder();
        PDFRenderer renderer = new PDFRenderer(document);

        System.out.println("Scanning " + document.getNumberOfPages() + " pages for images...");

        for (int page = 0; page < document.getNumberOfPages(); page++) {
            try {
                System.out.println("Analyzing page " + (page + 1) + "...");
                BufferedImage image = renderer.renderImageWithDPI(page, 200);
                String base64Image = imageToBase64(image);
                String description = analyzeImageWithGroq(base64Image, page + 1);

                if (description != null && !description.isEmpty()) {
                    imageDescriptions
                        .append("\n[Image on Page ").append(page + 1).append("]: ")
                        .append(description).append("\n");
                    System.out.println("Page " + (page + 1) + " analyzed successfully.");
                }
            } catch (Exception e) {
                System.err.println("Page " + (page + 1) + " analysis failed: " + e.getMessage());
            }
        }

        return imageDescriptions.toString();
    }

    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String analyzeImageWithGroq(String base64Image, int pageNumber) {
        try {
            String requestBody = String.format(
                "{\"model\":\"meta-llama/llama-4-scout-17b-16e-instruct\"," +
                "\"messages\":[{\"role\":\"user\",\"content\":[" +
                "{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:image/png;base64,%s\"}}," +
                "{\"type\":\"text\",\"text\":\"Describe this page in detail. Extract all text, numbers, chart data, and table values you can see.\"}]}]," +
                "\"max_tokens\":1000}", base64Image);

            Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + groqApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody,
                    MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Groq Vision error on page " + pageNumber + ": " + response.code());
                    return "";
                }
                JsonNode json = objectMapper.readTree(response.body().string());
                return json.path("choices").path(0).path("message").path("content").asText("");
            }
        } catch (Exception e) {
            System.err.println("Image analysis error page " + pageNumber + ": " + e.getMessage());
            return "";
        }
    }
}