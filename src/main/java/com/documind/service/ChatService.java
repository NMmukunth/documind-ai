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

        String context = ragService.retrieveRelevantContext(question);

        // If keyword search finds nothing, use FULL document text
        if (context == null || context.trim().isEmpty()) {
            context = ragService.getFullDocumentText();
        }

        String prompt = """
            You are a helpful AI assistant analyzing a document.
            Answer the question based ONLY on the document context below.
            If the question asks about a table, look for numbers and product names in the context.
            If you truly cannot find the answer, say "I could not find that in the document."
            
            Document context:
            %s
            
            Question: %s
            """.formatted(context, question);

        return callGroqApi(prompt);
    }
    public String summarizeDocument() {
        if (!ragService.hasDocumentLoaded()) {
            return "Please upload a PDF document first.";
        }
        String documentSample = ragService.getDocumentSample();
        String prompt = buildSummaryPrompt(documentSample);
        return callGroqApi(prompt);
    }

    private String callGroqApi(String prompt) {
        try {
            OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .apiKey(groqApiKey)
                .modelName("llama-3.1-8b-instant")
                .temperature(0.3)
                .maxTokens(1000)
                .build();

            return model.generate(prompt);
        } catch (Exception e) {
            System.err.println("Groq API error: " + e.getMessage());
            return "I encountered an error while processing your request. " +
                   "Please check your API key and try again.";
        }
    }

    private String buildQuestionPrompt(String question, String context) {
        return """
            You are DocuMind AI, a helpful document assistant.
            
            Answer questions based ONLY on the document context provided below.
            If the answer is not in the context, say "I could not find that information in the document."
            Do NOT make up information. Be concise and accurate.
            
            === DOCUMENT CONTEXT ===
            %s
            === END OF CONTEXT ===
            
            User Question: %s
            
            Answer:
            """.formatted(context, question);
    }

    private String buildSummaryPrompt(String documentText) {
        return """
            You are DocuMind AI. Create a comprehensive summary of the following document.
            
            Structure your summary with:
            OVERVIEW: What is this document about? (2-3 sentences)
            KEY POINTS: The most important points (bullet list)
            DETAILS: Any important data, figures, or specifics mentioned
            CONCLUSION: Main takeaway or conclusion
            
            Document Content:
            %s
            
            Summary:
            """.formatted(documentText);
    }
}
