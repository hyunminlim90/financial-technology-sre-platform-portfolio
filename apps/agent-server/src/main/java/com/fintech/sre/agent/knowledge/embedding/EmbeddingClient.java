package com.fintech.sre.agent.knowledge.embedding;

import java.util.List;

import reactor.core.publisher.Mono;

public interface EmbeddingClient {

	Mono<EmbeddingResult> embed(List<EmbeddingRequest> requests);
}
