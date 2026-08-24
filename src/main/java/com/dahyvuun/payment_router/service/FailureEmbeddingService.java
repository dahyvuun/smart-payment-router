package com.dahyvuun.payment_router.service;

import com.dahyvuun.payment_router.domain.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailureEmbeddingService {

    private final VectorStore vectorStore;

    /**
     * 결제 실패 정보를 임베딩하여 벡터 저장소에 저장
     */
    public void embedFailure(Transaction transaction, String paymentMethod, String failureReason) {
        try {
            String content = String.format(
                    "결제 실패 - 금액: %s %s, 결제수단: %s, 사유: %s",
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
            // 임베딩 저장 실패가 결제 흐름을 막으면 안 됨 - 로그만 남기고 무시
            log.error("Failed to save failure embedding for transaction {}: {}", transaction.getId(), e.getMessage());
        }
    }

    /**
     * 유사한 과거 실패 사례 검색
     */
    public List<Document> findSimilarFailures(String currentFailureReason, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(currentFailureReason)
                .topK(topK)
                .build();
        return vectorStore.similaritySearch(searchRequest);
}
}