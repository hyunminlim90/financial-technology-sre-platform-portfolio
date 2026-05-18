package com.fintech.sre.agent.postmortem.draft;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryPostmortemDraftStore implements PostmortemDraftStore {

	private final Map<String, PostmortemDraftRecord> records = new ConcurrentHashMap<>();

	@Override
	public Mono<PostmortemDraftRecord> save(PostmortemDraftRecord record) {
		if (record == null) {
			return Mono.empty();
		}

		records.put(record.postmortemDraftId(), record);
		return Mono.just(record);
	}

	@Override
	public Mono<PostmortemDraftRecord> findById(String postmortemDraftId) {
		PostmortemDraftRecord record = records.get(postmortemDraftId);
		return record == null ? Mono.empty() : Mono.just(record);
	}

	@Override
	public Flux<PostmortemDraftRecord> findByIncidentId(String incidentId) {
		return Flux.fromStream(records.values().stream()
				.filter(record -> incidentId != null && incidentId.equals(record.incidentId()))
				.sorted(Comparator.comparing(PostmortemDraftRecord::createdAt).reversed()));
	}

	@Override
	public Flux<PostmortemDraftRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(records.values().stream()
				.sorted(Comparator.comparing(PostmortemDraftRecord::createdAt).reversed())
				.limit(safeLimit));
	}
}
