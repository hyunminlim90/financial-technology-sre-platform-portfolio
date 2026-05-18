package com.fintech.sre.agent.recommendation.verification;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VerificationResultStore {

	Mono<VerificationResultRecord> save(VerificationResultRecord record);

	Mono<VerificationResultRecord> findById(String verificationResultId);

	Flux<VerificationResultRecord> findByIncidentId(String incidentId);

	Flux<VerificationResultRecord> findByRecommendationRecordId(String recommendationRecordId);

	Flux<VerificationResultRecord> findByExecutionResultId(String executionResultId);

	Flux<VerificationResultRecord> findRecent(int limit);
}
