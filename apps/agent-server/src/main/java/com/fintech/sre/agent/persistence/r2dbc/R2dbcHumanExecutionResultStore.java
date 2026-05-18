package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcHumanExecutionResultStore
		implements HumanExecutionResultStore {

	private final HumanExecutionResultR2dbcRepository repository;
	private final HumanExecutionResultEntityMapper mapper;

	public R2dbcHumanExecutionResultStore(
			HumanExecutionResultR2dbcRepository repository,
			HumanExecutionResultEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<HumanExecutionResultRecord> save(
			HumanExecutionResultRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<HumanExecutionResultRecord> findById(String executionResultId) {
		if (executionResultId == null || executionResultId.isBlank()) {
			return Mono.empty();
		}

		return repository.findById(executionResultId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<HumanExecutionResultRecord> findByExecutionPlanId(
			String executionPlanId
	) {
		if (executionPlanId == null || executionPlanId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByExecutionPlanIdOrderByRecordedAtDesc(executionPlanId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<HumanExecutionResultRecord> findByRecommendationRecordId(
			String recommendationRecordId
	) {
		if (recommendationRecordId == null || recommendationRecordId.isBlank()) {
			return Flux.empty();
		}

		return repository
				.findByRecommendationRecordIdOrderByRecordedAtDesc(
						recommendationRecordId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<HumanExecutionResultRecord> findByIncidentId(String incidentId) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByRecordedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<HumanExecutionResultRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByRecordedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
