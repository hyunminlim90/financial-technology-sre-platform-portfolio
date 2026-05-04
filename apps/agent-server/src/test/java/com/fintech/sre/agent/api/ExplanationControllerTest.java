package com.fintech.sre.agent.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExplanationControllerTest {

	private final ApplicationContext applicationContext;
	private WebTestClient webTestClient;

	ExplanationControllerTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
	}

	@Test
	void shouldExplainDecisionReportWithoutChangingSafetyFlags() {
		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-EXPLAIN-1",
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

		String reportId = webTestClient.get()
				.uri("/api/decision-reports/incidents/INC-EXPLAIN-1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].id").value(String.class, value -> assertThat(value).isNotBlank())
				.returnResult()
				.getResponseBodyContent() == null ? null : null;

		webTestClient.get()
				.uri("/api/decision-reports/incidents/INC-EXPLAIN-1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].id").value(String.class, id ->
						webTestClient.post()
								.uri("/api/explanations/decision-reports/{decisionReportId}", id)
								.contentType(MediaType.APPLICATION_JSON)
								.bodyValue("""
										{
										  "question": "왜 scale out은 허용되고 restart pod는 차단됐나요?"
										}
										""")
								.exchange()
								.expectStatus().isOk()
								.expectBody()
								.jsonPath("$.incidentId").isEqualTo("INC-EXPLAIN-1")
								.jsonPath("$.rootCauseInferred").isEqualTo(false)
								.jsonPath("$.actionDecisionMadeByLlm").isEqualTo(false)
								.jsonPath("$.requiresHumanReview").isEqualTo(true)
								.jsonPath("$.explanation").value(String.class, explanation ->
										assertThat(explanation).contains("Human Review"))
				);
	}
}
