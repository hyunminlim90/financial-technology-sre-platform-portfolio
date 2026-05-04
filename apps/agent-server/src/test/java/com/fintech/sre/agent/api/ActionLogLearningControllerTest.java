package com.fintech.sre.agent.api;

import static org.assertj.core.api.Assertions.assertThat;

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
class ActionLogLearningControllerTest {

	private final ApplicationContext applicationContext;
	private ObjectMapper objectMapper;
	private WebTestClient webTestClient;

	ActionLogLearningControllerTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
		this.objectMapper = applicationContext.getBean(ObjectMapper.class);
	}

	@Test
	void recommendedActionShouldBeLoggedAndPromotedToPostmortemRequiredAfterOutcome() throws Exception {
		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-ACTION-LOG-1",
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
				.uri("/api/action-logs/incidents/INC-ACTION-LOG-1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult()
				.getResponseBody();

		List<ActionLogResponse> logs = objectMapper.readValue(body, new TypeReference<>() {
		});
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).status().name()).isEqualTo("RECOMMENDED");

		String actionLogId = logs.get(0).id();

		webTestClient.post()
				.uri("/api/action-logs/{actionLogId}/approve", actionLogId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "reason": "Operator approved controlled mitigation"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("APPROVED_BY_HUMAN");

		webTestClient.post()
				.uri("/api/action-logs/{actionLogId}/outcome", actionLogId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "outcomeStatus": "ROLLED_BACK",
						  "outcomeSummary": "Mitigation increased downstream errors and was rolled back",
						  "observedSignals": ["error_rate_up", "rollback_executed"]
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("POSTMORTEM_REQUIRED")
				.jsonPath("$.postmortemRequired").isEqualTo(true);

		webTestClient.get()
				.uri("/api/action-logs/postmortem-required")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].incidentId").isEqualTo("INC-ACTION-LOG-1")
				.jsonPath("$[0].status").isEqualTo("POSTMORTEM_REQUIRED");
	}
}
