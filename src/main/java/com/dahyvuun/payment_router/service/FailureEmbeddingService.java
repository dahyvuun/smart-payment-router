package com.dahyvuun.payment_router.service;

import com.dahyvuun.payment_router.domain.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailureEmbeddingService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    /**
     * Embeds a payment failure and stores it in the vector store.
     * Embedding failures are caught and logged only — they must never
     * block or affect the core payment flow.
     */
    public void embedFailure(Transaction transaction, String paymentMethod, String failureReason) {
        try {
            String content = String.format(
                    "Payment failure - amount: %s %s, method: %s, reason: %s",
                    transaction.getAmount(), transaction.getCurrency(), paymentMethod, failureReason
            );

            Document document = new Document(content, Map.of(
                    "transactionId", transaction.getId(),
                    "paymentMethod", paymentMethod,
                    "amount", transaction.getAmount().toString(),
                    "currency", transaction.getCurrency()
            ));

            vectorStore.add(List.of(document));
            log.info("Failure embedding saved for transaction: {}", transaction.getId());
        } catch (Exception e) {
            log.error("Failed to save failure embedding for transaction {}: {}", transaction.getId(), e.getMessage());
        }
    }

    /**
     * Finds past failure cases similar to the given failure reason.
     */
    public List<Document> findSimilarFailures(String currentFailureReason, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(currentFailureReason)
                .topK(topK)
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * Retrieves similar past failures and asks the LLM to analyze the
     * likely root cause and recommend a course of action.
     */
    public String analyzeFailure(String currentFailureReason) {
        List<Document> similarDocs = findSimilarFailures(currentFailureReason, 5);

        String context = similarDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n- ", "- ", ""));

        String prompt = """
                You are a payment systems analyst. Below are past payment failure cases:
                %s

                Current failure reason: %s

                Based on the past cases above, respond in Korean with:
                1. The likely root cause of the failure
                2. Recommended action (e.g. switch to a different payment gateway, adjust retry policy, etc.)
                """.formatted(context, currentFailureReason);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}