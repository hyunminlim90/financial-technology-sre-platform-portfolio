package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcExecutionPlanStore implements ExecutionPlanStore {

	private final RecommendationExecutionPlanR2dbcRepository repository;
	private final RecommendationExecutionPlanEntityMapper mapper;

	public R2dbcExecutionPlanStore(
			RecommendationExecutionPlanR2dbcRepository repository,
			RecommendationExecutionPlanEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<RecommendationExecutionPlan> save(
			RecommendationExecutionPlan plan
	) {
		if (plan == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(plan))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<RecommendationExecutionPlan> findById(String executionPlanId) {
		return repository.findById(executionPlanId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<RecommendationExecutionPlan> findByRecommendationRecordId(
			String recommendationRecordId
	) {
		if (recommendationRecordId == null || recommendationRecordId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByRecommendationRecordIdOrderByCreatedAtDesc(
						recommendationRecordId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<RecommendationExecutionPlan> findByIncidentId(String incidentId) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<RecommendationExecutionPlan> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByCreatedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
