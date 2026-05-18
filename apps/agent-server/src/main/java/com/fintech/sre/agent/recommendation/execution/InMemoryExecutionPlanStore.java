package com.fintech.sre.agent.recommendation.execution;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryExecutionPlanStore implements ExecutionPlanStore {

	private final Map<String, RecommendationExecutionPlan> plans =
			new ConcurrentHashMap<>();

	@Override
	public Mono<RecommendationExecutionPlan> save(RecommendationExecutionPlan plan) {
		if (plan == null) {
			return Mono.empty();
		}

		plans.put(plan.executionPlanId(), plan);
		return Mono.just(plan);
	}

	@Override
	public Mono<RecommendationExecutionPlan> findById(String executionPlanId) {
		RecommendationExecutionPlan plan = plans.get(executionPlanId);
		return plan == null ? Mono.empty() : Mono.just(plan);
	}

	@Override
	public Flux<RecommendationExecutionPlan> findByRecommendationRecordId(
			String recommendationRecordId
	) {
		return Flux.fromStream(plans.values().stream()
				.filter(plan -> recommendationRecordId != null
						&& recommendationRecordId.equals(plan.recommendationRecordId()))
				.sorted(Comparator.comparing(RecommendationExecutionPlan::createdAt).reversed()));
	}

	@Override
	public Flux<RecommendationExecutionPlan> findByIncidentId(String incidentId) {
		return Flux.fromStream(plans.values().stream()
				.filter(plan -> incidentId != null && incidentId.equals(plan.incidentId()))
				.sorted(Comparator.comparing(RecommendationExecutionPlan::createdAt).reversed()));
	}

	@Override
	public Flux<RecommendationExecutionPlan> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(plans.values().stream()
				.sorted(Comparator.comparing(RecommendationExecutionPlan::createdAt).reversed())
				.limit(safeLimit));
	}
}
