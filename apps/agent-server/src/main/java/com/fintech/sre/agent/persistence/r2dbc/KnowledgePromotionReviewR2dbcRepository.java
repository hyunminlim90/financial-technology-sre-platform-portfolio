package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Profile("r2dbc")
public interface KnowledgePromotionReviewR2dbcRepository
		extends ReactiveCrudRepository<KnowledgePromotionReviewEntity, String> {

	Mono<KnowledgePromotionReviewEntity> findFirstByLearningCandidateIdOrderByReviewedAtDesc(
			String learningCandidateId
	);

	Flux<KnowledgePromotionReviewEntity> findByLearningCandidateIdOrderByReviewedAtDesc(
			String learningCandidateId
	);

	Flux<KnowledgePromotionReviewEntity> findByIncidentIdOrderByReviewedAtDesc(
			String incidentId
	);

	Flux<KnowledgePromotionReviewEntity> findTop500ByOrderByReviewedAtDesc();
}
