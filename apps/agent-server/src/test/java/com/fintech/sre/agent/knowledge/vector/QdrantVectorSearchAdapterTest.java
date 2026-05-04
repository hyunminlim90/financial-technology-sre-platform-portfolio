package com.fintech.sre.agent.knowledge.vector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.fintech.sre.agent.embedding.EmbeddingPort;
import com.fintech.sre.agent.embedding.EmbeddingRequest;
import com.fintech.sre.agent.embedding.EmbeddingResponse;
import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;
import com.fintech.sre.agent.knowledge.vector.qdrant.QdrantPayloadMapper;
import com.fintech.sre.agent.knowledge.vector.qdrant.QdrantProperties;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class QdrantVectorSearchAdapterTest {

	@Test
	void shouldCallReadOnlyQdrantSearchEndpointAndMapResponse() {
		AtomicReference<String> requestedUrl = new AtomicReference<>();
		AtomicReference<String> apiKeyHeader = new AtomicReference<>();
		AtomicReference<String> embeddedInput = new AtomicReference<>();

		ExchangeFunction exchangeFunction = request -> {
			requestedUrl.set(request.url().toString());
			apiKeyHeader.set(request.headers().getFirst("api-key"));
			return Mono.just(ClientResponse.create(HttpStatus.OK)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.body("""
							{
							  "result": [
							    {
							      "id": "scenario-payment-high-latency",
							      "score": 0.95,
							      "payload": {
							        "layer": "SCENARIO",
							        "path": "scenarios/payment-api/high-latency.md",
							        "title": "Payment API High Latency Scenario",
							        "contentSnippet": "Scenario snippet",
							        "domain": "payment",
							        "source": "portfolio"
							      }
							    },
							    {
							      "id": "runbook-payment-high-latency",
							      "score": 0.92,
							      "payload": {
							        "layer": "RUNBOOK",
							        "path": "runbooks/payment-api/high-latency.md",
							        "title": "Payment API High Latency Runbook",
							        "contentSnippet": "Runbook snippet",
							        "domain": "payment",
							        "source": "portfolio"
							      }
							    }
							  ]
							}
							""")
					.build());
		};

		WebClient webClient = WebClient.builder()
				.baseUrl("http://localhost:6333")
				.defaultHeader("api-key", "secret")
				.exchangeFunction(exchangeFunction)
				.build();
		EmbeddingPort embeddingPort = request -> {
			embeddedInput.set(request.input());
			return Mono.just(new EmbeddingResponse(List.of(0.11d, 0.22d, 0.33d), "test-embedding"));
		};

		QdrantVectorSearchAdapter adapter = new QdrantVectorSearchAdapter(
				webClient,
				new QdrantProperties("http://localhost:6333", "sre-knowledge", "secret", 10, 0.7),
				new QdrantPayloadMapper(),
				embeddingPort
		);

		StepVerifier.create(adapter.search(new VectorSearchRequest(
				"payment latency spike",
				List.of(KnowledgeLayer.SCENARIO, KnowledgeLayer.RUNBOOK),
				Map.of("domain", "payment"),
				5
		)))
				.assertNext(result -> {
					assertThat(result.documents()).hasSize(2);
					assertThat(result.documents().get(0).layer()).isEqualTo(KnowledgeLayer.SCENARIO);
					assertThat(result.documents().get(1).layer()).isEqualTo(KnowledgeLayer.RUNBOOK);
				})
				.verifyComplete();

		assertThat(requestedUrl.get()).isEqualTo("http://localhost:6333/collections/sre-knowledge/points/search");
		assertThat(apiKeyHeader.get()).isEqualTo("secret");
		assertThat(embeddedInput.get()).isEqualTo("payment latency spike");
	}
}
