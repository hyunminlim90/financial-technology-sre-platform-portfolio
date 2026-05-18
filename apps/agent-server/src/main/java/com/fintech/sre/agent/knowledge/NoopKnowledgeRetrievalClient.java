package com.fintech.sre.agent.knowledge;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@Profile("!qdrant")
public class NoopKnowledgeRetrievalClient implements KnowledgeRetrievalClient {

	@Override
	public Mono<KnowledgeSearchResult> search(KnowledgeSearchQuery query) {
		return Mono.just(KnowledgeSearchResult.empty());
	}
}
