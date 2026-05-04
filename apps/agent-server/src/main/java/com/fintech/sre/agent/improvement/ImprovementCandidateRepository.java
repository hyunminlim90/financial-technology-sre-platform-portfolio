package com.fintech.sre.agent.improvement;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImprovementCandidateRepository {

	Mono<ImprovementCandidate> save(ImprovementCandidate candidate);

	Mono<ImprovementCandidate> findById(String id);

	Flux<ImprovementCandidate> findByIncidentId(String incidentId);

	Flux<ImprovementCandidate> findByStatus(ImprovementCandidateStatus status);
}
