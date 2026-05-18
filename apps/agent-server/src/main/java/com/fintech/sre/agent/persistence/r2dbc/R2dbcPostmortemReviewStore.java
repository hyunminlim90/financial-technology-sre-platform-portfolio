package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcPostmortemReviewStore implements PostmortemReviewStore {

	private final PostmortemReviewR2dbcRepository repository;
	private final PostmortemReviewEntityMapper mapper;

	public R2dbcPostmortemReviewStore(
			PostmortemReviewR2dbcRepository repository,
			PostmortemReviewEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<PostmortemReviewRecord> save(PostmortemReviewRecord record) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Flux<PostmortemReviewRecord> findByDraftId(String postmortemDraftId) {
		if (postmortemDraftId == null || postmortemDraftId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByPostmortemDraftIdOrderByReviewedAtDesc(postmortemDraftId)
				.map(mapper::toDomain);
	}

	@Override
	public Mono<PostmortemReviewRecord> findLatestByDraftId(
			String postmortemDraftId
	) {
		if (postmortemDraftId == null || postmortemDraftId.isBlank()) {
			return Mono.empty();
		}

		return repository.findFirstByPostmortemDraftIdOrderByReviewedAtDesc(postmortemDraftId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<PostmortemReviewRecord> findByIncidentId(String incidentId) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByReviewedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<PostmortemReviewRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByReviewedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
