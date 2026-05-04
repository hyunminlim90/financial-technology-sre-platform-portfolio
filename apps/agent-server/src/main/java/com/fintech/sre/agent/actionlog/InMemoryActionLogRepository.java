package com.fintech.sre.agent.actionlog;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class InMemoryActionLogRepository implements ActionLogRepository {

	private final ConcurrentHashMap<String, ActionLog> store = new ConcurrentHashMap<>();

	@Override
	public Mono<ActionLog> save(ActionLog log) {
		store.put(log.id(), log);
		return Mono.just(log);
	}

	@Override
	public Mono<ActionLog> findById(String id) {
		ActionLog log = store.get(id);
		return log == null ? Mono.empty() : Mono.just(log);
	}

	@Override
	public Flux<ActionLog> findByIncidentId(String incidentId) {
		return Flux.fromIterable(store.values())
				.filter(log -> incidentId.equals(log.incidentId()));
	}

	@Override
	public Flux<ActionLog> findPostmortemRequired() {
		return Flux.fromIterable(store.values())
				.filter(ActionLog::postmortemRequired);
	}
}
