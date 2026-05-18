package com.fintech.sre.agent.recommendation.approval;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryRecommendationApprovalStore implements RecommendationApprovalStore {

	private final Map<String, RecommendationApprovalRecord> recordsByApprovalId =
			new ConcurrentHashMap<>();

	@Override
	public Mono<RecommendationApprovalRecord> save(RecommendationApprovalRecord record) {
		if (record == null) {
			return Mono.empty();
		}

		recordsByApprovalId.put(record.approvalId(), record);
		return Mono.just(record);
	}

	@Override
	public Mono<RecommendationApprovalRecord> findLatestByRecommendationRecordId(
			String recommendationRecordId
	) {
		return findByRecommendationRecordId(recommendationRecordId).next();
	}

	@Override
	public Flux<RecommendationApprovalRecord> findByRecommendationRecordId(
			String recommendationRecordId
	) {
		return Flux.fromIterable(recordsByApprovalId.values())
				.filter(record -> recommendationRecordId != null
						&& recommendationRecordId.equals(record.recommendationRecordId()))
				.sort(Comparator.comparing(RecommendationApprovalRecord::decidedAt).reversed());
	}

	@Override
	public Flux<RecommendationApprovalRecord> findByIncidentId(String incidentId) {
		return Flux.fromIterable(recordsByApprovalId.values())
				.filter(record -> incidentId != null && incidentId.equals(record.incidentId()))
				.sort(Comparator.comparing(RecommendationApprovalRecord::decidedAt).reversed());
	}

	@Override
	public Flux<RecommendationApprovalRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(recordsByApprovalId.values().stream()
				.sorted(Comparator.comparing(RecommendationApprovalRecord::decidedAt).reversed())
				.limit(safeLimit));
	}
}
