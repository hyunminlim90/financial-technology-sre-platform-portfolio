package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
@Profile("r2dbc")
public interface HumanExecutionResultR2dbcRepository
		extends ReactiveCrudRepository<HumanExecutionResultEntity, String> {

	Flux<HumanExecutionResultEntity> findByExecutionPlanIdOrderByRecordedAtDesc(
			String executionPlanId
	);

	Flux<HumanExecutionResultEntity> findByRecommendationRecordIdOrderByRecordedAtDesc(
			String recommendationRecordId
	);

	Flux<HumanExecutionResultEntity> findByIncidentIdOrderByRecordedAtDesc(
			String incidentId
	);

	Flux<HumanExecutionResultEntity> findTop500ByOrderByRecordedAtDesc();
}
