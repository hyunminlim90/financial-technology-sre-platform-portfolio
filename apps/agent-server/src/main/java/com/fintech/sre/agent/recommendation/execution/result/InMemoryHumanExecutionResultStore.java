package com.fintech.sre.agent.recommendation.execution.result;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryHumanExecutionResultStore implements HumanExecutionResultStore {

	private final Map<String, HumanExecutionResultRecord> records =
			new ConcurrentHashMap<>();

	@Override
	public Mono<HumanExecutionResultRecord> save(HumanExecutionResultRecord record) {
		if (record == null) {
			return Mono.empty();
		}

		records.put(record.executionResultId(), record);
		return Mono.just(record);
	}

	@Override
	public Mono<HumanExecutionResultRecord> findById(String executionResultId) {
		HumanExecutionResultRecord record = records.get(executionResultId);
		return record == null ? Mono.empty() : Mono.just(record);
	}

	@Override
	public Flux<HumanExecutionResultRecord> findByExecutionPlanId(String executionPlanId) {
		return find(record -> executionPlanId != null && executionPlanId.equals(record.executionPlanId()));
	}

	@Override
	public Flux<HumanExecutionResultRecord> findByRecommendationRecordId(String recommendationRecordId) {
		return find(record -> recommendationRecordId != null
				&& recommendationRecordId.equals(record.recommendationRecordId()));
	}

	@Override
	public Flux<HumanExecutionResultRecord> findByIncidentId(String incidentId) {
		return find(record -> incidentId != null && incidentId.equals(record.incidentId()));
	}

	@Override
	public Flux<HumanExecutionResultRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(records.values().stream()
				.sorted(Comparator.comparing(HumanExecutionResultRecord::recordedAt).reversed())
				.limit(safeLimit));
	}

	private Flux<HumanExecutionResultRecord> find(
			Predicate<HumanExecutionResultRecord> predicate
	) {
		return Flux.fromStream(records.values().stream()
				.filter(predicate)
				.sorted(Comparator.comparing(HumanExecutionResultRecord::recordedAt).reversed()));
	}
}
