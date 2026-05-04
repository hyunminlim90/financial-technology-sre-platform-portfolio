package com.fintech.sre.agent.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostmortemGenerationControllerTest {

	private final ApplicationContext applicationContext;
	private WebTestClient webTestClient;

	PostmortemGenerationControllerTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
	}

	@Test
	void generateReturnsDraftResponse() {
		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-3001",
						  "alertName": "RedisLatencyHigh",
						  "service": "payment-api",
						  "environment": "prod",
						  "severityHint": "SEV_2",
						  "occurredAt": "2026-05-02T00:00:00Z",
						  "metricsSnapshot": {
						    "p95LatencyMs": 420.0,
						    "redisTimeoutCount": 12,
						    "dbConnectionPending": 7,
						    "kafkaConsumerLag": 5000
						  },
						  "logsSample": [
						    {
						      "timestamp": "2026-05-02T00:05:00Z",
						      "level": "ERROR",
						      "message": "Redis command timed out",
						      "traceId": "trace-301"
						    }
						  ],
						  "traceIds": ["trace-301"],
						  "operatorNote": "Incident details captured during response"
						}
						""")
				.exchange()
				.expectStatus().isOk();

		webTestClient.post()
				.uri("/api/v1/postmortems/generate")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-3001",
						  "operatorSummary": "Incident closed after retry storm stabilized"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.incidentId").isEqualTo("INC-3001")
				.jsonPath("$.status").isEqualTo("DRAFT_CREATED")
				.jsonPath("$.humanValidationRequired").isEqualTo(true)
				.jsonPath("$.frontMatter.approvalStatus").isEqualTo("draft")
				.jsonPath("$.draft.timeline.length()").isEqualTo(1)
				.jsonPath("$.draft.rootCauseHypotheses.length()").isEqualTo(1)
				.jsonPath("$.improvementCandidates.length()").isEqualTo(0)
				.jsonPath("$.preventiveDesignCandidates.length()").isEqualTo(0);
	}
}
