package com.fintech.sre.agent.incident;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IncidentLifecycleRepository {

	Mono<IncidentLifecycle> save(IncidentLifecycle lifecycle);

	Mono<IncidentLifecycle> findByIncidentId(String incidentId);

	Flux<IncidentLifecycle> findAll();
}
