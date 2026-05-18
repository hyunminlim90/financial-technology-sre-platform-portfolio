package com.fintech.sre.agent.knowledge.embedding.local;

import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fintech.sre.agent.knowledge.embedding.EmbeddingClient;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingRequest;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingResult;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Profile("local-embedding")
public class LocalEmbeddingClient implements EmbeddingClient {

	private final WebClient webClient;
	private final LocalEmbeddingProperties properties;
	private final LocalEmbeddingResponseMapper mapper;

	public LocalEmbeddingClient(
			WebClient.Builder webClientBuilder,
			LocalEmbeddingProperties properties,
			LocalEmbeddingResponseMapper mapper
	) {
		this.properties = properties;
		this.mapper = mapper;
		this.webClient = webClientBuilder
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.build();
	}

	@Override
	public Mono<EmbeddingResult> embed(List<EmbeddingRequest> requests) {
		if (!properties.enabled()) {
			return Mono.just(mapper.failed(
					requests,
					"LOCAL_EMBEDDING_DISABLED",
					"Local embedding provider is disabled."
			));
		}

		if (requests == null || requests.isEmpty()) {
			return Mono.just(EmbeddingResult.empty());
		}

		LocalEmbeddingRequest body = new LocalEmbeddingRequest(
				properties.modelOrDefault(),
				requests.stream()
						.map(EmbeddingRequest::input)
						.toList()
		);

		return webClient.post()
				.uri("/v1/embeddings")
				.bodyValue(body)
				.retrieve()
				.bodyToMono(LocalEmbeddingResponse.class)
				.timeout(properties.timeoutOrDefault())
				.retryWhen(Retry.backoff(
						properties.maxRetriesOrDefault(),
						Duration.ofMillis(100)
				))
				.map(response -> mapper.toResult(requests, response))
				.onErrorReturn(mapper.failed(
						requests,
						"LOCAL_EMBEDDING_PROVIDER_FAILURE",
						"Local embedding provider failed. Chunks will not be upserted."
				));
	}
}
