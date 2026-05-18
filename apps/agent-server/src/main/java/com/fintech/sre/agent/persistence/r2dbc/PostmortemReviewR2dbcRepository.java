package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Profile("r2dbc")
public interface PostmortemReviewR2dbcRepository
		extends ReactiveCrudRepository<PostmortemReviewEntity, String> {

	Flux<PostmortemReviewEntity> findByPostmortemDraftIdOrderByReviewedAtDesc(
			String postmortemDraftId
	);

	Mono<PostmortemReviewEntity> findFirstByPostmortemDraftIdOrderByReviewedAtDesc(
			String postmortemDraftId
	);

	Flux<PostmortemReviewEntity> findByIncidentIdOrderByReviewedAtDesc(
			String incidentId
	);

	Flux<PostmortemReviewEntity> findTop500ByOrderByReviewedAtDesc();
}
