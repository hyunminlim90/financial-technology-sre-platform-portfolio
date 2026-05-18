package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Profile("r2dbc")
public class R2dbcRecommendationRecordStore implements RecommendationRecordStore {

	private final RecommendationRecordR2dbcRepository repository;
	private final RecommendationRecordEntityMapper mapper;

	public R2dbcRecommendationRecordStore(
			RecommendationRecordR2dbcRepository repository,
			RecommendationRecordEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<RecommendationRecord> save(RecommendationRecord record) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toRecord);
	}

	@Override
	public Mono<RecommendationRecord> findById(String recommendationRecordId) {
		return repository.findById(recommendationRecordId)
				.map(mapper::toRecord);
	}

	@Override
	public Flux<RecommendationRecord> findByIncidentId(String incidentId) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByGeneratedAtDesc(incidentId)
				.map(mapper::toRecord);
	}

	@Override
	public Flux<RecommendationRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 200);
		return repository.findRecent(safeLimit)
				.map(mapper::toRecord);
	}
}
