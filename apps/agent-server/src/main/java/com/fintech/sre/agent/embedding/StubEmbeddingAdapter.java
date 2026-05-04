package com.fintech.sre.agent.embedding;

import java.util.stream.IntStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
		name = "agent.embedding.provider",
		havingValue = "stub",
		matchIfMissing = true
)
public class StubEmbeddingAdapter implements EmbeddingPort {

	@Override
	public Mono<EmbeddingResponse> embed(EmbeddingRequest request) {
		return Mono.just(new EmbeddingResponse(
				IntStream.range(0, 384)
						.mapToObj(i -> 0.01d)
						.toList(),
				"stub-384"
		));
	}
}
