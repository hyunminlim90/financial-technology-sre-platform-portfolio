package com.fintech.sre.agent.recommendation.persistence;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationRecordStore {

	Mono<RecommendationRecord> save(RecommendationRecord record);

	Mono<RecommendationRecord> findById(String recommendationRecordId);

	Flux<RecommendationRecord> findByIncidentId(String incidentId);

	Flux<RecommendationRecord> findRecent(int limit);
}
