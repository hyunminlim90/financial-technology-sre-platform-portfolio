package com.fintech.sre.agent.knowledge;

import reactor.core.publisher.Mono;

public interface KnowledgeRetrievalClient {

	Mono<KnowledgeSearchResult> search(KnowledgeSearchQuery query);
}
