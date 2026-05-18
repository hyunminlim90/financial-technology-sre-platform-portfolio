package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class R2dbcIncidentLifecycleStore
		implements IncidentLifecycleStore {

	private final IncidentLifecycleR2dbcRepository repository;
	private final IncidentLifecycleEntityMapper mapper;

	public R2dbcIncidentLifecycleStore(
			IncidentLifecycleR2dbcRepository repository,
			IncidentLifecycleEntityMapper mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Mono<IncidentLifecycleRecord> save(
			IncidentLifecycleRecord record
	) {
		if (record == null) {
			return Mono.empty();
		}

		return repository.save(mapper.toEntity(record))
				.map(mapper::toDomain);
	}

	@Override
	public Mono<IncidentLifecycleRecord> findLatestByIncidentId(
			String incidentId
	) {
		if (incidentId == null || incidentId.isBlank()) {
			return Mono.empty();
		}

		return repository.findFirstByIncidentIdOrderByTransitionedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<IncidentLifecycleRecord> findByIncidentId(
			String incidentId
	) {
		if (incidentId == null || incidentId.isBlank()) {
			return Flux.empty();
		}

		return repository.findByIncidentIdOrderByTransitionedAtDesc(incidentId)
				.map(mapper::toDomain);
	}

	@Override
	public Flux<IncidentLifecycleRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
		return repository.findTop500ByOrderByTransitionedAtDesc()
				.take(safeLimit)
				.map(mapper::toDomain);
	}
}
