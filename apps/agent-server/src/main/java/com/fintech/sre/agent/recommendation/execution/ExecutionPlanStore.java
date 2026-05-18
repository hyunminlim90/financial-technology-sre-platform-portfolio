package com.fintech.sre.agent.recommendation.execution;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExecutionPlanStore {

	Mono<RecommendationExecutionPlan> save(RecommendationExecutionPlan plan);

	Mono<RecommendationExecutionPlan> findById(String executionPlanId);

	Flux<RecommendationExecutionPlan> findByRecommendationRecordId(String recommendationRecordId);

	Flux<RecommendationExecutionPlan> findByIncidentId(String incidentId);

	Flux<RecommendationExecutionPlan> findRecent(int limit);
}
