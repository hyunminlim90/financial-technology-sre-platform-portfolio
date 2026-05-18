package com.fintech.sre.agent.knowledge.vector.upsert;

import java.time.Duration;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fintech.sre.agent.knowledge.qdrant.QdrantProperties;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Profile("qdrant")
public class QdrantVectorStoreUpsertClient implements VectorStoreUpsertClient {

	private final WebClient webClient;
	private final QdrantProperties properties;
	private final QdrantPointMapper pointMapper;

	public QdrantVectorStoreUpsertClient(
			WebClient.Builder webClientBuilder,
			QdrantProperties properties,
			QdrantPointMapper pointMapper
	) {
		this.properties = properties;
		this.pointMapper = pointMapper;
		this.webClient = webClientBuilder
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.defaultHeaders(headers -> {
					if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
						headers.set("api-key", properties.apiKey());
					}
				})
				.build();
	}

	@Override
	public Mono<VectorUpsertResult> upsert(VectorUpsertRequest request) {
		if (!properties.enabled()) {
			return Mono.just(VectorUpsertResult.empty());
		}

		if (request == null || request.isEmpty()) {
			return Mono.just(VectorUpsertResult.empty());
		}

		QdrantUpsertRequest body = new QdrantUpsertRequest(
				request.vectors().stream()
						.filter(vector -> vector != null
								&& vector.vector() != null
								&& !vector.vector().isEmpty())
						.map(pointMapper::toPoint)
						.toList()
		);

		if (body.points().isEmpty()) {
			return Mono.just(VectorUpsertResult.failed(
					request.vectors().stream()
							.map(vector -> new VectorUpsertFailure(
									vector == null ? "unknown" : vector.chunkId(),
									"EMPTY_VECTOR",
									"Embedding vector is empty and cannot be upserted."
							))
							.toList()
			));
		}

		return webClient.put()
				.uri("/collections/{collection}/points?wait=true", properties.collection())
				.bodyValue(body)
				.retrieve()
				.bodyToMono(QdrantUpsertResponse.class)
				.timeout(properties.timeoutOrDefault())
				.retryWhen(Retry.backoff(
						properties.maxRetriesOrDefault(),
						Duration.ofMillis(100)
				))
				.map(response -> toResult(response, body))
				.onErrorReturn(toFailure(
						request,
						"QDRANT_UPSERT_FAILED",
						"Qdrant upsert failed. Vectors were not persisted."
				));
	}

	private VectorUpsertResult toResult(
			QdrantUpsertResponse response,
			QdrantUpsertRequest request
	) {
		if (response == null || !response.ok()) {
			return VectorUpsertResult.failed(
					request.points().stream()
							.map(point -> new VectorUpsertFailure(
									point.id(),
									"QDRANT_UPSERT_NOT_OK",
									"Qdrant upsert response was not ok."
							))
							.toList()
			);
		}

		return VectorUpsertResult.success(
				request.points().stream()
						.map(QdrantUpsertRequest.Point::id)
						.toList()
		);
	}

	private VectorUpsertResult toFailure(
			VectorUpsertRequest request,
			String reasonCode,
			String reason
	) {
		return VectorUpsertResult.failed(
				request.vectors().stream()
						.map(vector -> new VectorUpsertFailure(
								vector == null ? "unknown" : vector.chunkId(),
								reasonCode,
								reason
						))
						.toList()
		);
	}
}
