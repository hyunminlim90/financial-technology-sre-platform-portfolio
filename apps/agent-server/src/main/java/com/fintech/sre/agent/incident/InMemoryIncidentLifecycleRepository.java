package com.fintech.sre.agent.incident;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class InMemoryIncidentLifecycleRepository implements IncidentLifecycleRepository {

	private final ConcurrentHashMap<String, IncidentLifecycle> store = new ConcurrentHashMap<>();

	@Override
	public Mono<IncidentLifecycle> save(IncidentLifecycle lifecycle) {
		store.put(lifecycle.incidentId(), lifecycle);
		return Mono.just(lifecycle);
	}

	@Override
	public Mono<IncidentLifecycle> findByIncidentId(String incidentId) {
		IncidentLifecycle lifecycle = store.get(incidentId);
		return lifecycle == null ? Mono.empty() : Mono.just(lifecycle);
	}

	@Override
	public Flux<IncidentLifecycle> findAll() {
		return Flux.fromIterable(store.values());
	}
}
