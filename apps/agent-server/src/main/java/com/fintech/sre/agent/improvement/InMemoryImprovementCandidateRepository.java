package com.fintech.sre.agent.improvement;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class InMemoryImprovementCandidateRepository implements ImprovementCandidateRepository {

	private final ConcurrentHashMap<String, ImprovementCandidate> store = new ConcurrentHashMap<>();

	@Override
	public Mono<ImprovementCandidate> save(ImprovementCandidate candidate) {
		store.put(candidate.id(), candidate);
		return Mono.just(candidate);
	}

	@Override
	public Mono<ImprovementCandidate> findById(String id) {
		ImprovementCandidate candidate = store.get(id);
		return candidate == null ? Mono.empty() : Mono.just(candidate);
	}

	@Override
	public Flux<ImprovementCandidate> findByIncidentId(String incidentId) {
		return Flux.fromIterable(store.values())
				.filter(candidate -> incidentId.equals(candidate.incidentId()));
	}

	@Override
	public Flux<ImprovementCandidate> findByStatus(ImprovementCandidateStatus status) {
		return Flux.fromIterable(store.values())
				.filter(candidate -> status == candidate.status());
	}
}
