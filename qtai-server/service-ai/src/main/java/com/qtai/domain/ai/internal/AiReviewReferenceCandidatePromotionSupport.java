package com.qtai.domain.ai.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class AiReviewReferenceCandidatePromotionSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiReviewReferenceCandidatePromotionSupport() {
    }

    static String sourceFileHash(Path candidatePath) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(candidatePath));
            JsonNode sourceFileHash = root == null ? null : root.get("sourceFileHash");
            if (sourceFileHash == null || !sourceFileHash.isTextual() || sourceFileHash.asText().isBlank()) {
                throw new IllegalArgumentException("AI_REVIEW_REFERENCE_CANDIDATE_INDEX_INVALID");
            }
            return sourceFileHash.asText();
        } catch (IOException exception) {
            throw new IllegalArgumentException("AI_REVIEW_REFERENCE_CANDIDATE_INDEX_READ_FAILED", exception);
        }
    }
}
