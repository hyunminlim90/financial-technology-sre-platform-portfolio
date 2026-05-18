package com.fintech.sre.agent.incident.lifecycle;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryIncidentLifecycleStore
		implements IncidentLifecycleStore {

	private final List<IncidentLifecycleRecord> records =
			new CopyOnWriteArrayList<>();

	@Override
	public Mono<IncidentLifecycleRecord> save(
			IncidentLifecycleRecord record
	) {
		if (record != null) {
			records.add(record);
		}

		return Mono.justOrEmpty(record);
	}

	@Override
	public Mono<IncidentLifecycleRecord> findLatestByIncidentId(
			String incidentId
	) {
		return findByIncidentId(incidentId).next();
	}

	@Override
	public Flux<IncidentLifecycleRecord> findByIncidentId(
			String incidentId
	) {
		return Flux.fromStream(records.stream()
				.filter(record ->
						incidentId != null
								&& incidentId.equals(record.incidentId()))
				.sorted(Comparator.comparing(
						IncidentLifecycleRecord::transitionedAt
				).reversed()));
	}

	@Override
	public Flux<IncidentLifecycleRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(records.stream()
				.sorted(Comparator.comparing(
						IncidentLifecycleRecord::transitionedAt
				).reversed())
				.limit(safeLimit));
	}
}
