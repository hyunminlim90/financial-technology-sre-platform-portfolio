package com.fintech.sre.agent.postmortem.review;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PostmortemReviewStore {

	Mono<PostmortemReviewRecord> save(
			PostmortemReviewRecord record
	);

	Flux<PostmortemReviewRecord> findByDraftId(
			String postmortemDraftId
	);

	Mono<PostmortemReviewRecord> findLatestByDraftId(
			String postmortemDraftId
	);

	Flux<PostmortemReviewRecord> findByIncidentId(
			String incidentId
	);

	Flux<PostmortemReviewRecord> findRecent(int limit);
}
