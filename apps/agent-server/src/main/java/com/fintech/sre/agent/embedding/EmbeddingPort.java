package com.fintech.sre.agent.embedding;

import reactor.core.publisher.Mono;

public interface EmbeddingPort {

	Mono<EmbeddingResponse> embed(EmbeddingRequest request);
}
