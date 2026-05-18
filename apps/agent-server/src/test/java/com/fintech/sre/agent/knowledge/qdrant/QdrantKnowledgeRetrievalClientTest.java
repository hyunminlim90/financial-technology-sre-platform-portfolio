package com.fintech.sre.agent.knowledge.qdrant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class QdrantKnowledgeRetrievalClientTest {

	@Test
	void shouldScrollQdrantAndMapDocuments() {
		AtomicReference<String> requestedUrl = new AtomicReference<>();
		AtomicReference<String> apiKey = new AtomicReference<>();

		ExchangeFunction exchangeFunction = request -> {
			requestedUrl.set(request.url().toString());
			apiKey.set(request.headers().getFirst("api-key"));
			return Mono.just(ClientResponse.create(HttpStatus.OK)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.body("""
							{
							  "result": {
							    "points": [
							      {
							        "id": "doc-1",
							        "payload": {
							          "type": "RUNBOOK",
							          "title": "Payment Runbook",
							          "path": "runbooks/payment.yaml",
							          "domain": "payment",
							          "service": "payment-service",
							          "scenarioIds": ["scenario-1"],
							          "runbookIds": ["runbook-1"],
							          "evidenceCodes": ["ERROR_RATE_HIGH"],
							          "actionTypes": ["RATE_LIMIT"],
							          "summary": "Matched payment mitigation runbook."
							        }
							      }
							    ]
							  },
							  "status": "ok",
							  "time": 0.01
							}
							""")
					.build());
		};

		QdrantKnowledgeRetrievalClient client = new QdrantKnowledgeRetrievalClient(
				WebClient.builder().exchangeFunction(exchangeFunction),
				new QdrantProperties(
						true,
						"http://localhost:6333",
						"fin-sre-knowledge",
						"secret",
						Duration.ofMillis(800),
						10,
						1
				),
				new QdrantPayloadMapper()
		);

		StepVerifier.create(client.search(new com.fintech.sre.agent.knowledge.KnowledgeSearchQuery(
				"inc-1",
				"payment-service",
				"payment",
				null,
				List.of("scenario-1"),
				List.of("runbook-1"),
				List.of("ERROR_RATE_HIGH"),
				null,
				10
		)))
				.assertNext(result -> {
					assertThat(result.isEmpty()).isFalse();
					assertThat(result.documents()).hasSize(1);
					assertThat(result.documents().get(0).title()).isEqualTo("Payment Runbook");
				})
				.verifyComplete();

		assertThat(requestedUrl.get()).isEqualTo("http://localhost:6333/collections/fin-sre-knowledge/points/scroll");
		assertThat(apiKey.get()).isEqualTo("secret");
	}

	@Test
	void shouldFallbackToEmptyWhenQdrantFails() {
		ExchangeFunction exchangeFunction = request -> Mono.error(new IllegalStateException("qdrant down"));

		QdrantKnowledgeRetrievalClient client = new QdrantKnowledgeRetrievalClient(
				WebClient.builder().exchangeFunction(exchangeFunction),
				new QdrantProperties(
						true,
						"http://localhost:6333",
						"fin-sre-knowledge",
						null,
						Duration.ofMillis(50),
						10,
						0
				),
				new QdrantPayloadMapper()
		);

		StepVerifier.create(client.search(new com.fintech.sre.agent.knowledge.KnowledgeSearchQuery(
				"inc-1",
				"payment-service",
				"payment",
				null,
				List.of(),
				List.of(),
				List.of(),
				null,
				10
		)))
				.assertNext(result -> assertThat(result.isEmpty()).isTrue())
				.verifyComplete();
	}
}
