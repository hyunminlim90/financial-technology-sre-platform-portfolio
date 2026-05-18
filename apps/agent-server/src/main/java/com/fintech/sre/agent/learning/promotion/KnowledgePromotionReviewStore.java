package com.fintech.sre.agent.learning.promotion;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KnowledgePromotionReviewStore {

	Mono<KnowledgePromotionReviewRecord> save(KnowledgePromotionReviewRecord record);

	Mono<KnowledgePromotionReviewRecord> findLatestByLearningCandidateId(String learningCandidateId);

	Flux<KnowledgePromotionReviewRecord> findByLearningCandidateId(String learningCandidateId);

	Flux<KnowledgePromotionReviewRecord> findByIncidentId(String incidentId);

	Flux<KnowledgePromotionReviewRecord> findRecent(int limit);
}
