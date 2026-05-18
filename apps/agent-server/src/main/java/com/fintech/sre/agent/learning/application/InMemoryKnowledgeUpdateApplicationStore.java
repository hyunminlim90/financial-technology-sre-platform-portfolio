package com.fintech.sre.agent.learning.application;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryKnowledgeUpdateApplicationStore
		implements KnowledgeUpdateApplicationStore {

	private final Map<String, KnowledgeUpdateApplicationRecord> records =
			new ConcurrentHashMap<>();

	@Override
	public Mono<KnowledgeUpdateApplicationRecord> save(
			KnowledgeUpdateApplicationRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		records.put(
				record.knowledgeUpdateApplicationId(),
				record
		);

		return Mono.just(record);
	}

	@Override
	public Mono<KnowledgeUpdateApplicationRecord> findById(
			String knowledgeUpdateApplicationId
	) {
		KnowledgeUpdateApplicationRecord record =
				records.get(knowledgeUpdateApplicationId);

		return record == null
				? Mono.empty()
				: Mono.just(record);
	}

	@Override
	public Flux<KnowledgeUpdateApplicationRecord> findByIncidentId(
			String incidentId
	) {
		return Flux.fromStream(records.values().stream()
				.filter(record ->
						incidentId != null
								&& incidentId.equals(record.incidentId()))
				.sorted(Comparator.comparing(
						KnowledgeUpdateApplicationRecord::appliedAt
				).reversed()));
	}

	@Override
	public Flux<KnowledgeUpdateApplicationRecord> findByLearningCandidateId(
			String learningCandidateId
	) {
		return Flux.fromStream(records.values().stream()
				.filter(record ->
						learningCandidateId != null
								&& learningCandidateId.equals(
								record.learningCandidateId()))
				.sorted(Comparator.comparing(
						KnowledgeUpdateApplicationRecord::appliedAt
				).reversed()));
	}

	@Override
	public Flux<KnowledgeUpdateApplicationRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(records.values().stream()
				.sorted(Comparator.comparing(
						KnowledgeUpdateApplicationRecord::appliedAt
				).reversed())
				.limit(safeLimit));
	}
}
