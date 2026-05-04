package com.fintech.sre.agent.knowledge.vector;

import reactor.core.publisher.Mono;

public interface VectorSearchPort {

	Mono<VectorSearchResult> search(VectorSearchRequest request);
}
