package com.fintech.sre.agent.knowledge.rag;

import reactor.core.publisher.Mono;

public interface KnowledgeSearchClient {

	Mono<KnowledgeSearchResult> search(KnowledgeSearchRequest request);
}
