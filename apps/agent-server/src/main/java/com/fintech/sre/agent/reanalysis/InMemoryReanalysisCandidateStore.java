package com.fintech.sre.agent.reanalysis;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryReanalysisCandidateStore
		implements ReanalysisCandidateStore {

	private final Map<String, ReanalysisTriggerCandidate> candidates =
			new ConcurrentHashMap<>();

	@Override
	public Mono<ReanalysisTriggerCandidate> save(
			ReanalysisTriggerCandidate candidate
	) {
		if (candidate == null) {
			return Mono.empty();
		}

		candidates.put(
				candidate.reanalysisCandidateId(),
				candidate
		);

		return Mono.just(candidate);
	}

	@Override
	public Mono<ReanalysisTriggerCandidate> findById(
			String reanalysisCandidateId
	) {
		ReanalysisTriggerCandidate candidate =
				candidates.get(reanalysisCandidateId);

		return candidate == null
				? Mono.empty()
				: Mono.just(candidate);
	}

	@Override
	public Flux<ReanalysisTriggerCandidate> findByIncidentId(
			String incidentId
	) {
		return Flux.fromStream(candidates.values().stream()
				.filter(candidate ->
						incidentId != null
								&& incidentId.equals(candidate.incidentId()))
				.sorted(Comparator.comparing(
						ReanalysisTriggerCandidate::createdAt
				).reversed()));
	}
}
