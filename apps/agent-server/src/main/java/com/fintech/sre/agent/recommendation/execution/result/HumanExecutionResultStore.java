package com.fintech.sre.agent.recommendation.execution.result;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HumanExecutionResultStore {

	Mono<HumanExecutionResultRecord> save(HumanExecutionResultRecord record);

	Mono<HumanExecutionResultRecord> findById(String executionResultId);

	Flux<HumanExecutionResultRecord> findByExecutionPlanId(String executionPlanId);

	Flux<HumanExecutionResultRecord> findByRecommendationRecordId(String recommendationRecordId);

	Flux<HumanExecutionResultRecord> findByIncidentId(String incidentId);

	Flux<HumanExecutionResultRecord> findRecent(int limit);
}
