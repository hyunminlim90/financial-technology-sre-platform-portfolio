package com.fintech.sre.agent.knowledge.vector;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fintech.sre.agent.embedding.EmbeddingPort;
import com.fintech.sre.agent.embedding.EmbeddingRequest;
import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;
import com.fintech.sre.agent.knowledge.vector.qdrant.QdrantPayloadMapper;
import com.fintech.sre.agent.knowledge.vector.qdrant.QdrantSearchRequest;
import com.fintech.sre.agent.knowledge.vector.qdrant.QdrantSearchResponse;
import com.fintech.sre.agent.knowledge.vector.qdrant.VectorQdrantProperties;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
		name = "agent.knowledge.vector.adapter",
		havingValue = "qdrant"
)
public class QdrantVectorSearchAdapter implements VectorSearchPort {

	private final WebClient qdrantWebClient;
	private final VectorQdrantProperties properties;
	private final QdrantPayloadMapper payloadMapper;
	private final EmbeddingPort embeddingPort;

	public QdrantVectorSearchAdapter(
			WebClient qdrantWebClient,
			VectorQdrantProperties properties,
			QdrantPayloadMapper payloadMapper,
			EmbeddingPort embeddingPort
	) {
		this.qdrantWebClient = qdrantWebClient;
		this.properties = properties;
		this.payloadMapper = payloadMapper;
		this.embeddingPort = embeddingPort;
	}

	@Override
	public Mono<VectorSearchResult> search(VectorSearchRequest request) {
		return embeddingPort.embed(new EmbeddingRequest(request.query()))
				.flatMap(embedding -> {
					QdrantSearchRequest qdrantRequest = new QdrantSearchRequest(
							embedding.vector(),
							request.limit() > 0 ? request.limit() : properties.limit(),
							properties.scoreThreshold(),
							buildFilter(request),
							true
					);

					return qdrantWebClient.post()
							.uri("/collections/{collection}/points/search", properties.collection())
							.bodyValue(qdrantRequest)
							.retrieve()
							.bodyToMono(QdrantSearchResponse.class)
							.map(response -> new VectorSearchResult(
									response.result() == null
											? List.of()
											: response.result().stream()
													.map(payloadMapper::toDocument)
													.toList()
							));
				});
	}

	private Map<String, Object> buildFilter(VectorSearchRequest request) {
		if ((request.layers() == null || request.layers().isEmpty())
				&& (request.filters() == null || request.filters().isEmpty())) {
			return Map.of();
		}

		return Map.of("must", buildMustConditions(request));
	}

	private List<Map<String, Object>> buildMustConditions(VectorSearchRequest request) {
		java.util.ArrayList<Map<String, Object>> must = new java.util.ArrayList<>();

		if (request.layers() != null && !request.layers().isEmpty()) {
			must.add(Map.of(
					"key", "layer",
					"match", Map.of(
							"any", request.layers().stream()
									.map(KnowledgeLayer::name)
									.toList()
					)
			));
		}

		if (request.filters() != null) {
			request.filters().forEach((key, value) ->
					must.add(Map.of(
							"key", key,
							"match", Map.of("value", value)
					))
			);
		}

		return must;
	}
}
