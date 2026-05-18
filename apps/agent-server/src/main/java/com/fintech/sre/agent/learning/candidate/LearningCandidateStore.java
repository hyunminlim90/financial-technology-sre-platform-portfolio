package com.fintech.sre.agent.learning.candidate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LearningCandidateStore {

	Mono<LearningCandidateRecord> save(
			LearningCandidateRecord record
	);

	Mono<LearningCandidateRecord> findById(
			String learningCandidateId
	);

	Flux<LearningCandidateRecord> findByIncidentId(
			String incidentId
	);

	Flux<LearningCandidateRecord> findRecent(int limit);
}
