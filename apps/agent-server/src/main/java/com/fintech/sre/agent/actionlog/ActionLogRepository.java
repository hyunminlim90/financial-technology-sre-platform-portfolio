package com.fintech.sre.agent.actionlog;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ActionLogRepository {

	Mono<ActionLog> save(ActionLog log);

	Mono<ActionLog> findById(String id);

	Flux<ActionLog> findByIncidentId(String incidentId);

	Flux<ActionLog> findPostmortemRequired();
}
