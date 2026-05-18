package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcKnowledgePromotionReviewStore
		implements KnowledgePromotionReviewStore {

	private final KnowledgePromotionReviewR2dbcRepository repository;
	private final KnowledgePromotionReviewEntityMapper mapper;

	public R2dbcKnowledgePromotionReviewStore(
			KnowledgePromotionReviewR2dbcRepository repository,
			KnowledgePromotionReviewEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<KnowledgePromotionReviewRecord> save(
			KnowledgePromotionReviewRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<KnowledgePromotionReviewRecord> findLatestByLearningCandidateId(
			String learningCandidateId
	) {
		if (learningCandidateId == null || learningCandidateId.isBlank()) {
			return Mono.empty();
		}

		return repository.findFirstByLearningCandidateIdOrderByReviewedAtDesc(
						learningCandidateId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<KnowledgePromotionReviewRecord> findByLearningCandidateId(
			String learningCandidateId
	) {
		if (learningCandidateId == null || learningCandidateId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByLearningCandidateIdOrderByReviewedAtDesc(
						learningCandidateId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<KnowledgePromotionReviewRecord> findByIncidentId(
			String incidentId
	) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByReviewedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<KnowledgePromotionReviewRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByReviewedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
