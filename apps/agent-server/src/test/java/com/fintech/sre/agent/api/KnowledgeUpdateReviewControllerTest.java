package com.fintech.sre.agent.api;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.improvement.ImprovementCandidateResponse;
import com.fintech.sre.agent.knowledge.KnowledgeUpdateReviewResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KnowledgeUpdateReviewControllerTest {

	private final ApplicationContext applicationContext;
	private WebTestClient webTestClient;
	private ObjectMapper objectMapper;

	KnowledgeUpdateReviewControllerTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
		this.objectMapper = applicationContext.getBean(ObjectMapper.class);
	}

	@Test
	void shouldCreateKnowledgeUpdateReviewFromImprovementCandidate() throws Exception {
		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-KNOWLEDGE-API-1",
						  "alertName": "CheckoutHighLatency",
						  "service": "checkout-service",
						  "environment": "prod",
						  "severityHint": "SEV_2",
						  "occurredAt": "2026-05-02T00:00:00Z",
						  "metricsSnapshot": {
						    "p95LatencyMs": 920.0,
						    "errorRate": 0.12,
						    "retryRate": 0.28,
						    "dbConnectionPending": 7,
						    "kafkaConsumerLag": 5000
						  },
						  "logsSample": [
						    {
						      "timestamp": "2026-05-02T00:00:05Z",
						      "level": "ERROR",
						      "message": "TimeoutException at downstream payment client",
						      "traceId": "trace-001"
						    }
						  ],
						  "traceIds": ["trace-001"],
						  "operatorNote": "Checkout latency spike and timeout errors"
						}
						""")
				.exchange()
				.expectStatus().isOk();

		byte[] actionBody = webTestClient.get()
				.uri("/api/action-logs/incidents/INC-KNOWLEDGE-API-1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult()
				.getResponseBody();

		List<com.fintech.sre.agent.actionlog.ActionLogResponse> actionLogs = objectMapper.readValue(
				actionBody,
				new TypeReference<>() {
				}
		);

		webTestClient.post()
				.uri("/api/action-logs/{actionLogId}/outcome", actionLogs.get(0).id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "outcomeStatus": "ROLLED_BACK",
						  "outcomeSummary": "Mitigation rolled back after side effects",
						  "observedSignals": ["rollback_executed", "error_rate_up"]
						}
						""")
				.exchange()
				.expectStatus().isOk();

		byte[] candidateBody = webTestClient.post()
				.uri("/api/improvement-candidates/incidents/INC-KNOWLEDGE-API-1/generate")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult()
				.getResponseBody();

		List<ImprovementCandidateResponse> candidates = objectMapper.readValue(candidateBody, new TypeReference<>() {
		});

		webTestClient.post()
				.uri("/api/improvement-candidates/{candidateId}/accept", candidates.get(0).id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "reason": "Accepted for knowledge review"
						}
						""")
				.exchange()
				.expectStatus().isOk();

		byte[] reviewBody = webTestClient.post()
				.uri("/api/knowledge-update-reviews/improvement-candidates/{candidateId}/create", candidates.get(0).id())
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult()
				.getResponseBody();

		List<KnowledgeUpdateReviewResponse> reviews = objectMapper.readValue(reviewBody, new TypeReference<>() {
		});

		org.assertj.core.api.Assertions.assertThat(reviews).hasSize(1);

		webTestClient.get()
				.uri("/api/knowledge-update-reviews/requested")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].status").isEqualTo("REQUESTED");

		webTestClient.post()
				.uri("/api/knowledge-update-reviews/{reviewId}/applied-externally", reviews.get(0).id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "reason": "Applied in portfolio repo by human reviewer"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("APPLIED_EXTERNALLY");
	}
}
