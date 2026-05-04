package com.fintech.sre.agent.embedding;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
		name = "agent.embedding.provider",
		havingValue = "openai"
)
public class OpenAiEmbeddingAdapter implements EmbeddingPort {

	private final WebClient embeddingWebClient;
	private final EmbeddingProperties properties;

	public OpenAiEmbeddingAdapter(
			WebClient embeddingWebClient,
			EmbeddingProperties properties
	) {
		this.embeddingWebClient = embeddingWebClient;
		this.properties = properties;
	}

	@Override
	public Mono<EmbeddingResponse> embed(EmbeddingRequest request) {
		return embeddingWebClient.post()
				.uri("/embeddings")
				.bodyValue(new OpenAiEmbeddingRequest(
						properties.model(),
						request.input()
				))
				.retrieve()
				.bodyToMono(OpenAiEmbeddingResponse.class)
				.map(response -> new EmbeddingResponse(
						response.data().isEmpty()
								? List.of()
								: response.data().get(0).embedding(),
						properties.model()
				));
	}

	private record OpenAiEmbeddingRequest(
			String model,
			String input
	) {
	}

	private record OpenAiEmbeddingResponse(
			List<OpenAiEmbeddingData> data
	) {
	}

	private record OpenAiEmbeddingData(
			List<Double> embedding
	) {
	}
}
