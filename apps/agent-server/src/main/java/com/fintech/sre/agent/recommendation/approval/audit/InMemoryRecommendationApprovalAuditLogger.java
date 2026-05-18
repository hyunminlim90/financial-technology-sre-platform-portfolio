package com.fintech.sre.agent.recommendation.approval.audit;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryRecommendationApprovalAuditLogger
		implements RecommendationApprovalAuditLogger {

	private final List<RecommendationApprovalAuditLog> logs =
			new CopyOnWriteArrayList<>();

	@Override
	public Mono<Void> log(RecommendationApprovalAuditLog log) {
		if (log != null) {
			logs.add(log);
		}

		return Mono.empty();
	}

	@Override
	public Flux<RecommendationApprovalAuditLog> findByIncidentId(
			String incidentId
	) {
		return Flux.fromStream(logs.stream()
				.filter(log -> incidentId != null
						&& incidentId.equals(log.incidentId()))
				.sorted(Comparator.comparing(
						RecommendationApprovalAuditLog::decidedAt
				).reversed()));
	}

	@Override
	public Flux<RecommendationApprovalAuditLog> findByRecommendationRecordId(
			String recommendationRecordId
	) {
		return Flux.fromStream(logs.stream()
				.filter(log -> recommendationRecordId != null
						&& recommendationRecordId.equals(log.recommendationRecordId()))
				.sorted(Comparator.comparing(
						RecommendationApprovalAuditLog::decidedAt
				).reversed()));
	}
}
