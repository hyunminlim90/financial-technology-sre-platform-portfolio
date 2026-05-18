package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
@Profile("r2dbc")
public interface VerificationResultR2dbcRepository
		extends ReactiveCrudRepository<VerificationResultEntity, String> {

	Flux<VerificationResultEntity> findByExecutionResultIdOrderByVerifiedAtDesc(
			String executionResultId
	);

	Flux<VerificationResultEntity> findByRecommendationRecordIdOrderByVerifiedAtDesc(
			String recommendationRecordId
	);

	Flux<VerificationResultEntity> findByIncidentIdOrderByVerifiedAtDesc(
			String incidentId
	);

	Flux<VerificationResultEntity> findTop500ByOrderByVerifiedAtDesc();
}
