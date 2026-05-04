package com.fintech.sre.agent.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DecisionReportControllerTest {

	private final ApplicationContext applicationContext;
	private WebTestClient webTestClient;

	DecisionReportControllerTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
	}

	@Test
	void analyzeShouldCreateDecisionReport() {
		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-REPORT-1",
						  "alertName": "CheckoutHighLatency",
						  "service": "payment-service",
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

		webTestClient.get()
				.uri("/api/decision-reports/incidents/INC-REPORT-1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].incidentId").isEqualTo("INC-REPORT-1")
				.jsonPath("$[0].status").isEqualTo("HUMAN_REVIEW_REQUIRED")
				.jsonPath("$[0].evidenceSignals.length()").isNotEmpty()
				.jsonPath("$[0].actions.length()").isNotEmpty()
				.jsonPath("$[0].knowledgeLayeringIssues.length()").isNotEmpty()
				.jsonPath("$[0].humanReviewRequirements.length()").isNotEmpty()
				.jsonPath("$[0].markdown").value(String.class, markdown -> {
					org.assertj.core.api.Assertions.assertThat(markdown).contains("# Decision Report");
					org.assertj.core.api.Assertions.assertThat(markdown).contains("## 5. Knowledge Layering Issues");
					org.assertj.core.api.Assertions.assertThat(markdown).contains("AI did not execute any action.");
				});
	}
}
