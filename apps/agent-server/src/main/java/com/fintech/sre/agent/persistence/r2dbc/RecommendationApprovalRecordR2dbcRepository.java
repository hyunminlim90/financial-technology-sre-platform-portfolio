package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Profile("r2dbc")
public interface RecommendationApprovalRecordR2dbcRepository
		extends ReactiveCrudRepository<RecommendationApprovalRecordEntity, String> {

	Flux<RecommendationApprovalRecordEntity> findByIncidentIdOrderByDecidedAtDesc(
			String incidentId
	);

	Flux<RecommendationApprovalRecordEntity> findByRecommendationRecordIdOrderByDecidedAtDesc(
			String recommendationRecordId
	);

	Mono<RecommendationApprovalRecordEntity> findFirstByRecommendationRecordIdOrderByDecidedAtDesc(
			String recommendationRecordId
	);

	Flux<RecommendationApprovalRecordEntity> findTop500ByOrderByDecidedAtDesc();
}
