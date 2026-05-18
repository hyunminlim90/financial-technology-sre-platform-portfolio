package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
@Profile("r2dbc")
public interface KnowledgeUpdateApplicationR2dbcRepository
		extends ReactiveCrudRepository<KnowledgeUpdateApplicationEntity, String> {

	Flux<KnowledgeUpdateApplicationEntity> findByIncidentIdOrderByAppliedAtDesc(
			String incidentId
	);

	Flux<KnowledgeUpdateApplicationEntity> findByLearningCandidateIdOrderByAppliedAtDesc(
			String learningCandidateId
	);

	Flux<KnowledgeUpdateApplicationEntity> findTop500ByOrderByAppliedAtDesc();
}
