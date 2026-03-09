package com.documind.service;

import com.documind.rag.RagService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    @Autowired
    private RagService ragService;

    @Value("${groq.api.key}")
    private String groqApiKey;

    public String askQuestion(String question) {
        if (!ragService.hasDocumentLoaded()) {
            return "Please upload a PDF first.";
        }

        // Step 1 — Try vector search first
        String context = ragService
            .retrieveRelevantContext(question);

        System.out.println("Vector search result length: "
            + (context != null ? context.length() : 0));

        // Step 2 — If nothing found use full document
        if (context == null || context.trim().isEmpty()) {
            System.out.println(
                "No vector match — using full document");
            context = ragService.getFullDocumentText();
        }

        // Step 3 — Trim context if too large
        // Groq has token limits
        if (context.length() > 6000) {
            context = context.substring(0, 6000);
            System.out.println("Context trimmed to 6000 chars");
        }

        // Step 4 — Build strong prompt
        String prompt = """
            You are DocuMind AI, a helpful document assistant.
            
            IMPORTANT RULES:
            - Answer ONLY from the document context below
            - If the context contains [Image on Page X],
              use that to answer image/chart questions
            - Give specific numbers and names from the document
            - If truly not found, say so briefly
            
            === DOCUMENT CONTEXT START ===
            %s
            === DOCUMENT CONTEXT END ===
            
            Question: %s
            
            Answer:
            """.formatted(context, question);

        System.out.println("Sending to Groq — prompt length: "
            + prompt.length());

        return callGroqApi(prompt);
    }

    public String summarizeDocument() {
        if (!ragService.hasDocumentLoaded()) {
            return "Please upload a PDF document first.";
        }

        // Use full text for summarization
        String documentText = ragService.getFullDocumentText();

        // Trim if too large
        if (documentText.length() > 6000) {
            documentText = documentText.substring(0, 6000);
        }

        String prompt = """
            You are DocuMind AI. Create a comprehensive 
            summary of the following document.
            
            Structure your summary with:
            OVERVIEW: What is this document about?
            KEY POINTS: Most important points
            DETAILS: Important data, figures, specifics
            CONCLUSION: Main takeaway
            
            Document:
            %s
            
            Summary:
            """.formatted(documentText);

        return callGroqApi(prompt);
    }

    private String callGroqApi(String prompt) {
        try {
            OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .apiKey(groqApiKey)
                .modelName("llama-3.3-70b-versatile")
                .temperature(0.3)
                .maxTokens(1000)
                .build();

            String response = model.generate(prompt);
            System.out.println("Groq responded successfully");
            return response;

        } catch (Exception e) {
            System.err.println("Groq API error: " 
                + e.getMessage());
            return "Error connecting to AI. " +
                   "Please check your API key.";
        }
    }
}