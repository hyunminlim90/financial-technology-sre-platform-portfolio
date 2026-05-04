package com.fintech.sre.agent.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IncidentRecommendationControllerTest {

	private final ApplicationContext applicationContext;
	private WebTestClient webTestClient;

	IncidentRecommendationControllerTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
	}

	@Test
	void analyzeReturnsRecommendationPayload() {
		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-1001",
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
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.incidentId").isEqualTo("INC-1001")
				.jsonPath("$.status").isEqualTo("RECOMMENDATION_CREATED")
				.jsonPath("$.humanApprovalRequired").isEqualTo(true)
				.jsonPath("$.confidenceLevel").isEqualTo("HIGH")
				.jsonPath("$.recommendedActions.length()").isEqualTo(1)
				.jsonPath("$.recommendedActions[0].rollbackPlan").exists()
				.jsonPath("$.recommendedActions[0].verification.length()").isEqualTo(1)
				.jsonPath("$.recommendedActions[0].command.type").isEqualTo("RATE_LIMIT")
				.jsonPath("$.forbiddenActions.length()").isEqualTo(1);
	}

	@Test
	void analyzeRejectsRequestWithoutAnyEvidenceSource() {
		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-2001",
						  "alertName": "CheckoutHighLatency",
						  "service": "checkout-service",
						  "environment": "prod",
						  "occurredAt": "2026-05-02T00:00:00Z"
						}
						""")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errorId").isNotEmpty()
				.jsonPath("$.message").isEqualTo("Insufficient evidence")
				.jsonPath("$.details[0].code").isEqualTo("INSUFFICIENT_EVIDENCE")
				.jsonPath("$.details[0].severity").isEqualTo("ERROR")
				.jsonPath("$.humanActionRequired").isEqualTo("Provide additional observability evidence and retry.");
	}
}
