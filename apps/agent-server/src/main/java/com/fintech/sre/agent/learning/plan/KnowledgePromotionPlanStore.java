package com.fintech.sre.agent.learning.plan;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KnowledgePromotionPlanStore {

	Mono<KnowledgePromotionPlanRecord> save(KnowledgePromotionPlanRecord record);

	Mono<KnowledgePromotionPlanRecord> findById(String promotionPlanId);

	Flux<KnowledgePromotionPlanRecord> findByLearningCandidateId(String learningCandidateId);

	Flux<KnowledgePromotionPlanRecord> findByIncidentId(String incidentId);

	Flux<KnowledgePromotionPlanRecord> findRecent(int limit);
}
