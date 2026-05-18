package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcKnowledgeUpdateApplicationStore
		implements KnowledgeUpdateApplicationStore {

	private final KnowledgeUpdateApplicationR2dbcRepository repository;
	private final KnowledgeUpdateApplicationEntityMapper mapper;

	public R2dbcKnowledgeUpdateApplicationStore(
			KnowledgeUpdateApplicationR2dbcRepository repository,
			KnowledgeUpdateApplicationEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<KnowledgeUpdateApplicationRecord> save(
			KnowledgeUpdateApplicationRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<KnowledgeUpdateApplicationRecord> findById(
			String knowledgeUpdateApplicationId
	) {
		if (knowledgeUpdateApplicationId == null
				|| knowledgeUpdateApplicationId.isBlank()) {
			return Mono.empty();
		}

		return repository.findById(knowledgeUpdateApplicationId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<KnowledgeUpdateApplicationRecord> findByIncidentId(
			String incidentId
	) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByAppliedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<KnowledgeUpdateApplicationRecord> findByLearningCandidateId(
			String learningCandidateId
	) {
		if (learningCandidateId == null || learningCandidateId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByLearningCandidateIdOrderByAppliedAtDesc(
						learningCandidateId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<KnowledgeUpdateApplicationRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByAppliedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
