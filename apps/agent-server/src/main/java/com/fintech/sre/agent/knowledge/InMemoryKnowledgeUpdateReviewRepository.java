package com.fintech.sre.agent.knowledge;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class InMemoryKnowledgeUpdateReviewRepository implements KnowledgeUpdateReviewRepository {

	private final ConcurrentHashMap<String, KnowledgeUpdateReview> store = new ConcurrentHashMap<>();

	@Override
	public Mono<KnowledgeUpdateReview> save(KnowledgeUpdateReview review) {
		store.put(review.id(), review);
		return Mono.just(review);
	}

	@Override
	public Mono<KnowledgeUpdateReview> findById(String id) {
		KnowledgeUpdateReview review = store.get(id);
		return review == null ? Mono.empty() : Mono.just(review);
	}

	@Override
	public Flux<KnowledgeUpdateReview> findByIncidentId(String incidentId) {
		return Flux.fromIterable(store.values())
				.filter(review -> incidentId.equals(review.incidentId()));
	}

	@Override
	public Flux<KnowledgeUpdateReview> findByStatus(KnowledgeUpdateStatus status) {
		return Flux.fromIterable(store.values())
				.filter(review -> status == review.status());
	}

	@Override
	public Flux<KnowledgeUpdateReview> findByImprovementCandidateId(String improvementCandidateId) {
		return Flux.fromIterable(store.values())
				.filter(review -> improvementCandidateId.equals(review.improvementCandidateId()));
	}
}
