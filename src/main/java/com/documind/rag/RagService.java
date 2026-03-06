package com.documind.rag;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RagService - Simple keyword-based retrieval.
 * No ONNX, no embedding model needed.
 * Splits document into chunks and finds relevant ones by keyword matching.
 */
@Service
public class RagService {

    private List<String> chunks = new ArrayList<>();
    private String fullDocumentText = "";
    private boolean documentLoaded = false;

    public void loadDocument(String text) {
        System.out.println("Loading document into RAG pipeline...");

        this.chunks = new ArrayList<>();
        this.fullDocumentText = text;

        // Split into chunks of ~500 characters, respecting sentence boundaries
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > 500 && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }

        // Add the last chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        this.documentLoaded = true;
        System.out.println("Document loaded! " + chunks.size() + " chunks created.");
    }

    public String retrieveRelevantContext(String question) {
        if (chunks.isEmpty()) return "";

        // Convert question to lowercase keywords
        String[] keywords = question.toLowerCase()
            .replaceAll("[^a-zA-Z0-9 ]", "")
            .split("\\s+");

        // Score each chunk by how many keywords it contains
        List<ScoredChunk> scored = new ArrayList<>();
        for (String chunk : chunks) {
            String lowerChunk = chunk.toLowerCase();
            int score = 0;
            for (String keyword : keywords) {
            	if (keyword.length() > 2 && lowerChunk.contains(keyword)) {
                    score++;
                }
            }
            scored.add(new ScoredChunk(chunk, score));
        }

        // Sort by score descending
        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        // Return top 3 chunks that have at least 1 keyword match
        return scored.stream()
            .filter(sc -> sc.score > 0)
            .limit(3)
            .map(sc -> sc.chunk)
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    public String getDocumentSample() {
        if (fullDocumentText.isEmpty()) return "";
        int maxChars = 4000;
        if (fullDocumentText.length() <= maxChars) return fullDocumentText;
        int cutoff = fullDocumentText.lastIndexOf('.', maxChars);
        if (cutoff == -1) cutoff = maxChars;
        return fullDocumentText.substring(0, cutoff + 1) + "\n\n[Document continues...]";
    }

    public boolean hasDocumentLoaded() {
        return documentLoaded;
    }
    public String getFullDocumentText() {
        return fullDocumentText;
    }

    // Simple inner class to hold chunk + score
    private static class ScoredChunk {
        String chunk;
        int score;
        ScoredChunk(String chunk, int score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}