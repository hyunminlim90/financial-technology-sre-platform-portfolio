package com.fintech.sre.agent.recommendation.approval;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationApprovalStore {

	Mono<RecommendationApprovalRecord> save(RecommendationApprovalRecord record);

	Mono<RecommendationApprovalRecord> findLatestByRecommendationRecordId(String recommendationRecordId);

	Flux<RecommendationApprovalRecord> findByRecommendationRecordId(String recommendationRecordId);

	Flux<RecommendationApprovalRecord> findByIncidentId(String incidentId);

	Flux<RecommendationApprovalRecord> findRecent(int limit);
}
