package com.fintech.sre.agent.knowledge;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KnowledgeUpdateReviewRepository {

	Mono<KnowledgeUpdateReview> save(KnowledgeUpdateReview review);

	Mono<KnowledgeUpdateReview> findById(String id);

	Flux<KnowledgeUpdateReview> findByIncidentId(String incidentId);

	Flux<KnowledgeUpdateReview> findByStatus(KnowledgeUpdateStatus status);

	Flux<KnowledgeUpdateReview> findByImprovementCandidateId(String improvementCandidateId);
}
