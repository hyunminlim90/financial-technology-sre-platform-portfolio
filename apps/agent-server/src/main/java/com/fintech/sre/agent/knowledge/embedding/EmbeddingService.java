package com.fintech.sre.agent.knowledge.embedding;

import java.util.List;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class EmbeddingService {

	private final EmbeddingClient embeddingClient;

	public EmbeddingService(EmbeddingClient embeddingClient) {
		this.embeddingClient = embeddingClient;
	}

	public Mono<EmbeddingResult> embed(List<EmbeddingRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return Mono.just(EmbeddingResult.empty());
		}

		return embeddingClient.embed(requests)
				.onErrorReturn(toProviderFailure(requests));
	}

	private EmbeddingResult toProviderFailure(List<EmbeddingRequest> requests) {
		return EmbeddingResult.failed(
				requests.stream()
						.map(request -> new EmbeddingFailure(
								request.chunkId(),
								"EMBEDDING_PROVIDER_FAILURE",
								"Embedding provider failed. Chunk will not be upserted."
						))
						.toList()
		);
	}
}
