package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcKnowledgePromotionPlanStore
		implements KnowledgePromotionPlanStore {

	private final KnowledgePromotionPlanR2dbcRepository repository;
	private final KnowledgePromotionPlanEntityMapper mapper;

	public R2dbcKnowledgePromotionPlanStore(
			KnowledgePromotionPlanR2dbcRepository repository,
			KnowledgePromotionPlanEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<KnowledgePromotionPlanRecord> save(
			KnowledgePromotionPlanRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<KnowledgePromotionPlanRecord> findById(String promotionPlanId) {
		if (promotionPlanId == null || promotionPlanId.isBlank()) {
			return Mono.empty();
		}

		return repository.findById(promotionPlanId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<KnowledgePromotionPlanRecord> findByLearningCandidateId(
			String learningCandidateId
	) {
		if (learningCandidateId == null || learningCandidateId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByLearningCandidateIdOrderByCreatedAtDesc(
						learningCandidateId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<KnowledgePromotionPlanRecord> findByIncidentId(String incidentId) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<KnowledgePromotionPlanRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByCreatedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
