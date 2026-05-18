package com.fintech.sre.agent.knowledge.qdrant;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fintech.sre.agent.knowledge.KnowledgeDocument;
import com.fintech.sre.agent.knowledge.KnowledgeRetrievalClient;
import com.fintech.sre.agent.knowledge.KnowledgeSearchQuery;
import com.fintech.sre.agent.knowledge.KnowledgeSearchResult;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Profile("qdrant")
public class QdrantKnowledgeRetrievalClient implements KnowledgeRetrievalClient {

	private final WebClient webClient;
	private final QdrantProperties properties;
	private final QdrantPayloadMapper payloadMapper;

	public QdrantKnowledgeRetrievalClient(
			WebClient.Builder webClientBuilder,
			QdrantProperties properties,
			QdrantPayloadMapper payloadMapper
	) {
		this.properties = properties;
		this.payloadMapper = payloadMapper;
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
	public Mono<KnowledgeSearchResult> search(KnowledgeSearchQuery query) {
		if (!properties.enabled()) {
			return Mono.just(KnowledgeSearchResult.empty());
		}

		QdrantScrollRequest request = new QdrantScrollRequest(
				buildFilter(query),
				properties.limitOrDefault(),
				true
		);

		return webClient.post()
				.uri("/collections/{collection}/points/scroll", properties.collection())
				.bodyValue(request)
				.retrieve()
				.bodyToMono(QdrantScrollResponse.class)
				.timeout(properties.timeoutOrDefault())
				.retryWhen(Retry.backoff(
								properties.maxRetriesOrDefault(),
								Duration.ofMillis(100)
						)
						.filter(this::isRetryable))
				.map(this::toSearchResult)
				.onErrorReturn(KnowledgeSearchResult.empty());
	}

	private boolean isRetryable(Throwable throwable) {
		return true;
	}

	private KnowledgeSearchResult toSearchResult(QdrantScrollResponse response) {
		if (response == null
				|| response.result() == null
				|| response.result().points() == null) {
			return KnowledgeSearchResult.empty();
		}

		List<KnowledgeDocument> documents = response.result().points().stream()
				.map(point -> payloadMapper.toDocument(point.id(), point.payload()))
				.filter(document -> document != null)
				.toList();

		return new KnowledgeSearchResult(documents);
	}

	private Map<String, Object> buildFilter(KnowledgeSearchQuery query) {
		Map<String, Object> filter = new LinkedHashMap<>();
		List<Map<String, Object>> must = new java.util.ArrayList<>();

		if (query == null) {
			filter.put("must", must);
			return filter;
		}

		addMatch(must, "domain", query.domain());
		addMatch(must, "service", query.service());

		if (query.scenarioIds() != null && !query.scenarioIds().isEmpty()) {
			addAny(must, "scenarioIds", query.scenarioIds());
		}

		if (query.runbookIds() != null && !query.runbookIds().isEmpty()) {
			addAny(must, "runbookIds", query.runbookIds());
		}

		if (query.evidenceCodes() != null && !query.evidenceCodes().isEmpty()) {
			addAny(must, "evidenceCodes", query.evidenceCodes());
		}

		filter.put("must", must);
		return filter;
	}

	private void addMatch(
			List<Map<String, Object>> must,
			String key,
			String value
	) {
		if (value == null || value.isBlank()) {
			return;
		}

		must.add(Map.of(
				"key", key,
				"match", Map.of("value", value)
		));
	}

	private void addAny(
			List<Map<String, Object>> must,
			String key,
			List<String> values
	) {
		must.add(Map.of(
				"key", key,
				"match", Map.of("any", values)
		));
	}
}
