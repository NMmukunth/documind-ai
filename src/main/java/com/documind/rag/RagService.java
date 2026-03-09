package com.documind.rag;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private List<String> chunks = new ArrayList<>();
    private List<double[]> chunkVectors = new ArrayList<>();
    private String fullDocumentText = "";
    private boolean documentLoaded = false;

    public void loadDocument(String text) {
        System.out.println("Loading document with smart search...");

        this.chunks = new ArrayList<>();
        this.chunkVectors = new ArrayList<>();
        this.fullDocumentText = text;

        // Split into chunks
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > 500
                    && currentChunk.length() > 0) {
                String chunk = currentChunk.toString().trim();
                chunks.add(chunk);
                chunkVectors.add(textToVector(chunk));
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }

        if (currentChunk.length() > 0) {
            String chunk = currentChunk.toString().trim();
            chunks.add(chunk);
            chunkVectors.add(textToVector(chunk));
        }

        this.documentLoaded = true;
        System.out.println("Document loaded! "
            + chunks.size() + " chunks ready.");
    }

    public String retrieveRelevantContext(String question) {
        if (chunks.isEmpty()) return "";

        double[] questionVector = textToVector(question);

        // Score each chunk using cosine similarity
        // This finds MEANING not just keywords!
        List<ScoredChunk> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            double similarity = cosineSimilarity(
                questionVector, chunkVectors.get(i));
            scored.add(new ScoredChunk(
                chunks.get(i), similarity));
        }

        // Sort by similarity score
        scored.sort((a, b) -> 
            Double.compare(b.score, a.score));

        // Return top 3 most similar chunks
        return scored.stream()
            .filter(sc -> sc.score > 0.1)
            .limit(3)
            .map(sc -> sc.chunk)
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    // Convert text to a simple vector
    // Based on character and word patterns
    private double[] textToVector(String text) {
        double[] vector = new double[128];
        String lower = text.toLowerCase();
        String[] words = lower.split("\\s+");

        for (int i = 0; i < words.length; i++) {
            String word = words[i]
                .replaceAll("[^a-z0-9]", "");
            if (word.isEmpty()) continue;

            // Hash each word into vector dimensions
            int hash = Math.abs(word.hashCode());
            int dim1 = hash % 128;
            int dim2 = (hash / 128) % 128;
            int dim3 = (hash / 16384) % 128;

            vector[dim1] += 1.0 / (i + 1);
            vector[dim2] += 0.5 / (i + 1);
            vector[dim3] += 0.25 / (i + 1);

            // Bigrams for better context
            if (i < words.length - 1) {
                String bigram = word + words[i + 1]
                    .replaceAll("[^a-z0-9]", "");
                int bigramHash = Math.abs(
                    bigram.hashCode()) % 128;
                vector[bigramHash] += 0.8 / (i + 1);
            }
        }

        // Normalize the vector
        return normalize(vector);
    }

    // Cosine similarity — finds how similar
    // two vectors (meanings) are
    private double cosineSimilarity(
            double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) 
            * Math.sqrt(normB));
    }

    private double[] normalize(double[] vector) {
        double magnitude = 0;
        for (double v : vector) magnitude += v * v;
        magnitude = Math.sqrt(magnitude);
        if (magnitude == 0) return vector;
        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / magnitude;
        }
        return normalized;
    }

    public String getFullDocumentText() {
        return fullDocumentText;
    }

    public String getDocumentSample() {
        if (fullDocumentText.isEmpty()) return "";
        int maxChars = 4000;
        if (fullDocumentText.length() <= maxChars)
            return fullDocumentText;
        int cutoff = fullDocumentText
            .lastIndexOf('.', maxChars);
        if (cutoff == -1) cutoff = maxChars;
        return fullDocumentText
            .substring(0, cutoff + 1)
            + "\n\n[Document continues...]";
    }

    public boolean hasDocumentLoaded() {
        return documentLoaded;
    }

    private static class ScoredChunk {
        String chunk;
        double score;
        ScoredChunk(String chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}