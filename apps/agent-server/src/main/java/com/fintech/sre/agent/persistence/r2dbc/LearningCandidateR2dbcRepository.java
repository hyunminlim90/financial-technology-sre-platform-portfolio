package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
@Profile("r2dbc")
public interface LearningCandidateR2dbcRepository
		extends ReactiveCrudRepository<LearningCandidateEntity, String> {

	Flux<LearningCandidateEntity> findByIncidentIdOrderByCreatedAtDesc(
			String incidentId
	);

	Flux<LearningCandidateEntity> findTop500ByOrderByCreatedAtDesc();
}
