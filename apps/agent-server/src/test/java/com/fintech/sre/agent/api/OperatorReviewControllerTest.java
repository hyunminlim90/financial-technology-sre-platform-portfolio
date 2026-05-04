package com.fintech.sre.agent.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OperatorReviewControllerTest {

	private final ApplicationContext applicationContext;
	private WebTestClient webTestClient;

	OperatorReviewControllerTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
	}

	@Test
	void shouldProvideOperatorReviewSummaryAndRecordHumanDecisions() {
		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-OPERATOR-1",
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
				.uri("/api/operator/incidents/INC-OPERATOR-1/review")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.incidentId").isEqualTo("INC-OPERATOR-1")
				.jsonPath("$.actionLogs.length()").isNotEmpty()
				.jsonPath("$.decisionReports.length()").isNotEmpty()
				.jsonPath("$.improvementCandidates.length()").isEqualTo(0)
				.jsonPath("$.knowledgeUpdateReviews.length()").isEqualTo(0)
				.jsonPath("$.actionLogs[0].id").value(String.class, actionLogId -> {
					assertThat(actionLogId).isNotBlank();

					webTestClient.post()
							.uri("/api/operator/action-logs/{actionLogId}/approve", actionLogId)
							.contentType(MediaType.APPLICATION_JSON)
							.bodyValue("""
									{
									  "reason": "Operator approved after reviewing rollback and verification."
									}
									""")
							.exchange()
							.expectStatus().isOk()
							.expectBody()
							.jsonPath("$.status").isEqualTo("APPROVED_BY_HUMAN")
							.jsonPath("$.humanDecisionReason").isEqualTo("Operator approved after reviewing rollback and verification.");

					webTestClient.post()
							.uri("/api/operator/action-logs/{actionLogId}/outcome", actionLogId)
							.contentType(MediaType.APPLICATION_JSON)
							.bodyValue("""
									{
									  "outcomeStatus": "PARTIALLY_MITIGATED",
									  "outcomeSummary": "Latency improved but duplicate prevention review is still needed.",
									  "observedSignals": ["latency.p95", "payment.consistency"]
									}
									""")
							.exchange()
							.expectStatus().isOk()
							.expectBody()
							.jsonPath("$.status").isEqualTo("POSTMORTEM_REQUIRED")
							.jsonPath("$.outcomeStatus").isEqualTo("PARTIALLY_MITIGATED")
							.jsonPath("$.postmortemRequired").isEqualTo(true);
				});

		webTestClient.get()
				.uri("/api/incidents/INC-OPERATOR-1/lifecycle")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.incidentId").isEqualTo("INC-OPERATOR-1")
				.jsonPath("$.status").isEqualTo("OUTCOME_REPORTED")
				.jsonPath("$.history.length()").isNotEmpty();
	}
}
