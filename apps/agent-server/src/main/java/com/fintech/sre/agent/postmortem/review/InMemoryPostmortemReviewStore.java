package com.fintech.sre.agent.postmortem.review;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryPostmortemReviewStore
		implements PostmortemReviewStore {

	private final List<PostmortemReviewRecord> records =
			new CopyOnWriteArrayList<>();

	@Override
	public Mono<PostmortemReviewRecord> save(
			PostmortemReviewRecord record
	) {
		if (record != null) {
			records.add(record);
		}

		return Mono.justOrEmpty(record);
	}

	@Override
	public Flux<PostmortemReviewRecord> findByDraftId(
			String postmortemDraftId
	) {
		return Flux.fromStream(records.stream()
				.filter(record ->
						postmortemDraftId != null
								&& postmortemDraftId.equals(
								record.postmortemDraftId()
						))
				.sorted(Comparator.comparing(
						PostmortemReviewRecord::reviewedAt
				).reversed()));
	}

	@Override
	public Mono<PostmortemReviewRecord> findLatestByDraftId(
			String postmortemDraftId
	) {
		return findByDraftId(postmortemDraftId).next();
	}

	@Override
	public Flux<PostmortemReviewRecord> findByIncidentId(
			String incidentId
	) {
		return Flux.fromStream(records.stream()
				.filter(record ->
						incidentId != null
								&& incidentId.equals(record.incidentId()))
				.sorted(Comparator.comparing(
						PostmortemReviewRecord::reviewedAt
				).reversed()));
	}

	@Override
	public Flux<PostmortemReviewRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(records.stream()
				.sorted(Comparator.comparing(
						PostmortemReviewRecord::reviewedAt
				).reversed())
				.limit(safeLimit));
	}
}
