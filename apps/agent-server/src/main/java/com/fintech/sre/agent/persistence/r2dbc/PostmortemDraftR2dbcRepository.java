package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
@Profile("r2dbc")
public interface PostmortemDraftR2dbcRepository
		extends ReactiveCrudRepository<PostmortemDraftEntity, String> {

	Flux<PostmortemDraftEntity> findByIncidentIdOrderByCreatedAtDesc(
			String incidentId
	);

	Flux<PostmortemDraftEntity> findTop500ByOrderByCreatedAtDesc();
}
