package com.fintech.sre.agent.recommendation.verification;

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
public class InMemoryVerificationResultStore
		implements VerificationResultStore {

	private final Map<String, VerificationResultRecord> records =
			new ConcurrentHashMap<>();

	@Override
	public Mono<VerificationResultRecord> save(
			VerificationResultRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		records.put(record.verificationResultId(), record);

		return Mono.just(record);
	}

	@Override
	public Mono<VerificationResultRecord> findById(
			String verificationResultId
	) {
		VerificationResultRecord record = records.get(verificationResultId);

		return record == null
				? Mono.empty()
				: Mono.just(record);
	}

	@Override
	public Flux<VerificationResultRecord> findByIncidentId(
			String incidentId
	) {
		return find(record ->
				incidentId != null
						&& incidentId.equals(record.incidentId())
		);
	}

	@Override
	public Flux<VerificationResultRecord> findByRecommendationRecordId(
			String recommendationRecordId
	) {
		return find(record ->
				recommendationRecordId != null
						&& recommendationRecordId.equals(
						record.recommendationRecordId()
				)
		);
	}

	@Override
	public Flux<VerificationResultRecord> findByExecutionResultId(
			String executionResultId
	) {
		return find(record ->
				executionResultId != null
						&& executionResultId.equals(record.executionResultId())
		);
	}

	@Override
	public Flux<VerificationResultRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(records.values().stream()
				.sorted(Comparator.comparing(VerificationResultRecord::verifiedAt).reversed())
				.limit(safeLimit));
	}

	private Flux<VerificationResultRecord> find(
			Predicate<VerificationResultRecord> predicate
	) {
		return Flux.fromStream(records.values().stream()
				.filter(predicate)
				.sorted(Comparator.comparing(
						VerificationResultRecord::verifiedAt
				).reversed()));
	}
}
