package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcVerificationResultStore
		implements VerificationResultStore {

	private final VerificationResultR2dbcRepository repository;
	private final VerificationResultEntityMapper mapper;

	public R2dbcVerificationResultStore(
			VerificationResultR2dbcRepository repository,
			VerificationResultEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<VerificationResultRecord> save(
			VerificationResultRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<VerificationResultRecord> findById(String verificationResultId) {
		if (verificationResultId == null || verificationResultId.isBlank()) {
			return Mono.empty();
		}

		return repository.findById(verificationResultId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<VerificationResultRecord> findByIncidentId(String incidentId) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByVerifiedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<VerificationResultRecord> findByRecommendationRecordId(
			String recommendationRecordId
	) {
		if (recommendationRecordId == null || recommendationRecordId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByRecommendationRecordIdOrderByVerifiedAtDesc(
						recommendationRecordId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<VerificationResultRecord> findByExecutionResultId(
			String executionResultId
	) {
		if (executionResultId == null || executionResultId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByExecutionResultIdOrderByVerifiedAtDesc(
						executionResultId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<VerificationResultRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByVerifiedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
