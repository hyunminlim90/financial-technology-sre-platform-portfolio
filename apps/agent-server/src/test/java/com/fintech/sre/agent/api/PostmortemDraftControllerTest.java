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
import com.fintech.sre.agent.actionlog.ActionLogResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostmortemDraftControllerTest {

	private final ApplicationContext applicationContext;
	private ObjectMapper objectMapper;
	private WebTestClient webTestClient;

	PostmortemDraftControllerTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
		this.objectMapper = applicationContext.getBean(ObjectMapper.class);
	}

	@Test
	void shouldGenerateMarkdownDraftWithPendingRootCause() throws Exception {
		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-DRAFT-1",
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

		byte[] body = webTestClient.get()
				.uri("/api/action-logs/incidents/INC-DRAFT-1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult()
				.getResponseBody();

		List<ActionLogResponse> actionLogs = objectMapper.readValue(body, new TypeReference<>() {
		});
		String actionLogId = actionLogs.get(0).id();

		webTestClient.post()
				.uri("/api/action-logs/{actionLogId}/outcome", actionLogId)
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

		webTestClient.get()
				.uri("/api/postmortem-drafts/incidents/INC-DRAFT-1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.incidentId").isEqualTo("INC-DRAFT-1")
				.jsonPath("$.rootCause").isEqualTo("확인 필요")
				.jsonPath("$.requiresHumanReview").isEqualTo(true)
				.jsonPath("$.markdown").value(String.class, markdown -> {
					org.assertj.core.api.Assertions.assertThat(markdown).contains("# Postmortem Draft");
					org.assertj.core.api.Assertions.assertThat(markdown).contains("Root Cause: **확인 필요**");
				});
	}
}
