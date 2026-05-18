package com.fintech.sre.agent.reanalysis;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReanalysisCandidateStore {

	Mono<ReanalysisTriggerCandidate> save(
			ReanalysisTriggerCandidate candidate
	);

	Mono<ReanalysisTriggerCandidate> findById(
			String reanalysisCandidateId
	);

	Flux<ReanalysisTriggerCandidate> findByIncidentId(
			String incidentId
	);
}
