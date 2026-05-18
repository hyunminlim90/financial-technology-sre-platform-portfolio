package com.fintech.sre.agent.recommendation.persistence;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryRecommendationRecordStore implements RecommendationRecordStore {

	private final Map<String, RecommendationRecord> records = new ConcurrentHashMap<>();

	@Override
	public Mono<RecommendationRecord> save(RecommendationRecord record) {
		if (record == null) {
			return Mono.empty();
		}

		records.put(record.recommendationRecordId(), record);
		return Mono.just(record);
	}

	@Override
	public Mono<RecommendationRecord> findById(String recommendationRecordId) {
		RecommendationRecord record = records.get(recommendationRecordId);
		return record == null ? Mono.empty() : Mono.just(record);
	}

	@Override
	public Flux<RecommendationRecord> findByIncidentId(String incidentId) {
		return Flux.fromStream(records.values().stream()
				.filter(record -> incidentId != null && incidentId.equals(record.incidentId()))
				.sorted(Comparator.comparing(RecommendationRecord::generatedAt).reversed()));
	}

	@Override
	public Flux<RecommendationRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 200);

		return Flux.fromStream(records.values().stream()
				.sorted(Comparator.comparing(RecommendationRecord::generatedAt).reversed())
				.limit(safeLimit));
	}
}
