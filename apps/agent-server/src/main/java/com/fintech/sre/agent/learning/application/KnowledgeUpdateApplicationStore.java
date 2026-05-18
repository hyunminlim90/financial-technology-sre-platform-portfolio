package com.fintech.sre.agent.learning.application;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KnowledgeUpdateApplicationStore {

	Mono<KnowledgeUpdateApplicationRecord> save(
			KnowledgeUpdateApplicationRecord record
	);

	Mono<KnowledgeUpdateApplicationRecord> findById(
			String knowledgeUpdateApplicationId
	);

	Flux<KnowledgeUpdateApplicationRecord> findByIncidentId(
			String incidentId
	);

	Flux<KnowledgeUpdateApplicationRecord> findByLearningCandidateId(
			String learningCandidateId
	);

	Flux<KnowledgeUpdateApplicationRecord> findRecent(int limit);
}
