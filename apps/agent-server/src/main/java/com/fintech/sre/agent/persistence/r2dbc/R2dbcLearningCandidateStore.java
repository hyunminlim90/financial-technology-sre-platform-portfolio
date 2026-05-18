package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcLearningCandidateStore implements LearningCandidateStore {

	private final LearningCandidateR2dbcRepository repository;
	private final LearningCandidateEntityMapper mapper;

	public R2dbcLearningCandidateStore(
			LearningCandidateR2dbcRepository repository,
			LearningCandidateEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<LearningCandidateRecord> save(LearningCandidateRecord record) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<LearningCandidateRecord> findById(String learningCandidateId) {
		if (learningCandidateId == null || learningCandidateId.isBlank()) {
			return Mono.empty();
		}

		return repository.findById(learningCandidateId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<LearningCandidateRecord> findByIncidentId(String incidentId) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<LearningCandidateRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByCreatedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
