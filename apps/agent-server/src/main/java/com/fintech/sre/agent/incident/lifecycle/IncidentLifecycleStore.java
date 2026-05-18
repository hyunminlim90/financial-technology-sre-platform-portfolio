package com.fintech.sre.agent.incident.lifecycle;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IncidentLifecycleStore {

	Mono<IncidentLifecycleRecord> save(IncidentLifecycleRecord record);

	Mono<IncidentLifecycleRecord> findLatestByIncidentId(String incidentId);

	Flux<IncidentLifecycleRecord> findByIncidentId(String incidentId);

	Flux<IncidentLifecycleRecord> findRecent(int limit);
}
