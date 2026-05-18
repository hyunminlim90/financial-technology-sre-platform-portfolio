package com.fintech.sre.agent.postmortem.draft;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PostmortemDraftStore {

	Mono<PostmortemDraftRecord> save(PostmortemDraftRecord record);

	Mono<PostmortemDraftRecord> findById(String postmortemDraftId);

	Flux<PostmortemDraftRecord> findByIncidentId(String incidentId);

	Flux<PostmortemDraftRecord> findRecent(int limit);
}
