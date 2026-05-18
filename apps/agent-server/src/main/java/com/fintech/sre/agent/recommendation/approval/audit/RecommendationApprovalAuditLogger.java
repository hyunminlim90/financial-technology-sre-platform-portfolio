package com.fintech.sre.agent.recommendation.approval.audit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationApprovalAuditLogger {

	Mono<Void> log(RecommendationApprovalAuditLog log);

	Flux<RecommendationApprovalAuditLog> findByIncidentId(String incidentId);

	Flux<RecommendationApprovalAuditLog> findByRecommendationRecordId(
			String recommendationRecordId
	);
}
