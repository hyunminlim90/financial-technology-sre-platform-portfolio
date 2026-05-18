package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcRecommendationApprovalStore
		implements RecommendationApprovalStore {

	private final RecommendationApprovalRecordR2dbcRepository repository;
	private final RecommendationApprovalRecordEntityMapper mapper;

	public R2dbcRecommendationApprovalStore(
			RecommendationApprovalRecordR2dbcRepository repository,
			RecommendationApprovalRecordEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<RecommendationApprovalRecord> save(
			RecommendationApprovalRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<RecommendationApprovalRecord> findLatestByRecommendationRecordId(
			String recommendationRecordId
	) {
		if (recommendationRecordId == null || recommendationRecordId.isBlank()) {
			return Mono.empty();
		}

		return repository
				.findFirstByRecommendationRecordIdOrderByDecidedAtDesc(
						recommendationRecordId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<RecommendationApprovalRecord> findByIncidentId(
			String incidentId
	) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByDecidedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<RecommendationApprovalRecord> findByRecommendationRecordId(
			String recommendationRecordId
	) {
		if (recommendationRecordId == null || recommendationRecordId.isBlank()) {
			return Flux.empty();
		}

		return repository
				.findByRecommendationRecordIdOrderByDecidedAtDesc(
						recommendationRecordId
				)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<RecommendationApprovalRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByDecidedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
