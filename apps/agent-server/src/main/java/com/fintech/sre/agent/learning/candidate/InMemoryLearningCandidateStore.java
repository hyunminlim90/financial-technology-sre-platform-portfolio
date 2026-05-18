package com.fintech.sre.agent.learning.candidate;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryLearningCandidateStore
		implements LearningCandidateStore {

	private final Map<String, LearningCandidateRecord> records =
			new ConcurrentHashMap<>();

	@Override
	public Mono<LearningCandidateRecord> save(
			LearningCandidateRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		records.put(record.learningCandidateId(), record);

		return Mono.just(record);
	}

	@Override
	public Mono<LearningCandidateRecord> findById(
			String learningCandidateId
	) {
		LearningCandidateRecord record =
				records.get(learningCandidateId);

		return record == null
				? Mono.empty()
				: Mono.just(record);
	}

	@Override
	public Flux<LearningCandidateRecord> findByIncidentId(
			String incidentId
	) {
		return Flux.fromStream(records.values().stream()
				.filter(record ->
						incidentId != null
								&& incidentId.equals(record.incidentId()))
				.sorted(Comparator.comparing(
						LearningCandidateRecord::createdAt
				).reversed()));
	}

	@Override
	public Flux<LearningCandidateRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(records.values().stream()
				.sorted(Comparator.comparing(
						LearningCandidateRecord::createdAt
				).reversed())
				.limit(safeLimit));
	}
}
