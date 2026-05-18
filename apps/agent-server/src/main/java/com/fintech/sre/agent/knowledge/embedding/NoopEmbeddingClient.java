package com.fintech.sre.agent.knowledge.embedding;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@Profile("!stub-embedding & !local-embedding")
public class NoopEmbeddingClient implements EmbeddingClient {

	@Override
	public Mono<EmbeddingResult> embed(List<EmbeddingRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return Mono.just(EmbeddingResult.empty());
		}

		return Mono.just(EmbeddingResult.failed(
				requests.stream()
						.map(request -> new EmbeddingFailure(
								request.chunkId(),
								"EMBEDDING_PROVIDER_NOT_CONFIGURED",
								"No embedding provider configured. Chunk will not be upserted."
						))
						.toList()
		));
	}
}
