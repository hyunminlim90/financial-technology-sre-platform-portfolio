package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Profile("r2dbc")
public interface IncidentLifecycleR2dbcRepository
		extends ReactiveCrudRepository<IncidentLifecycleEntity, String> {

	Mono<IncidentLifecycleEntity> findFirstByIncidentIdOrderByTransitionedAtDesc(
			String incidentId
	);

	Flux<IncidentLifecycleEntity> findByIncidentIdOrderByTransitionedAtDesc(
			String incidentId
	);

	Flux<IncidentLifecycleEntity> findTop500ByOrderByTransitionedAtDesc();
}
