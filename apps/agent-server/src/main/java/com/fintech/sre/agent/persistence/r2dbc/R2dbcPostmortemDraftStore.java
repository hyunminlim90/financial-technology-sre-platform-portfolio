package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcPostmortemDraftStore implements PostmortemDraftStore {

	private final PostmortemDraftR2dbcRepository repository;
	private final PostmortemDraftEntityMapper mapper;

	public R2dbcPostmortemDraftStore(
			PostmortemDraftR2dbcRepository repository,
			PostmortemDraftEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<PostmortemDraftRecord> save(PostmortemDraftRecord record) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<PostmortemDraftRecord> findById(String postmortemDraftId) {
		if (postmortemDraftId == null || postmortemDraftId.isBlank()) {
			return Mono.empty();
		}

		return repository.findById(postmortemDraftId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<PostmortemDraftRecord> findByIncidentId(String incidentId) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<PostmortemDraftRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByCreatedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
