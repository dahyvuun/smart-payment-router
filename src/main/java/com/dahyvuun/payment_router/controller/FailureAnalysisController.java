package com.dahyvuun.payment_router.controller;

import com.dahyvuun.payment_router.dto.FailureAnalysisResponse;
import com.dahyvuun.payment_router.service.FailureEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FailureAnalysisController {

    private final FailureEmbeddingService failureEmbeddingService;

    /**
     * Takes a payment failure reason, retrieves similar past failures,
     * and returns an LLM-generated root cause analysis with recommendations.
     */
    @GetMapping("/api/v1/admin/failures/analyze")
    public FailureAnalysisResponse analyzeFailure(@RequestParam String reason) {
        List<Document> similarDocs = failureEmbeddingService.findSimilarFailures(reason, 5);
        List<String> similarTexts = similarDocs.stream()
                .map(Document::getText)
                .toList();

        String analysis = failureEmbeddingService.analyzeFailure(reason);

        return new FailureAnalysisResponse(reason, similarTexts, analysis);
    }
}