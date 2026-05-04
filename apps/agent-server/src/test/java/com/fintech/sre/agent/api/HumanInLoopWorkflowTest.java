package com.fintech.sre.agent.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.improvement.ImprovementCandidateResponse;
import com.fintech.sre.agent.knowledge.KnowledgeUpdateReviewResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HumanInLoopWorkflowTest {

	private final ApplicationContext applicationContext;
	private WebTestClient webTestClient;
	private ObjectMapper objectMapper;

	HumanInLoopWorkflowTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
		this.objectMapper = applicationContext.getBean(ObjectMapper.class);
	}

	@Test
	void shouldCreateLearningArtifactsWithoutExecutingActions() throws Exception {
		String incidentId = "INC-HITL-1";

		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-HITL-1",
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
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("RECOMMENDATION_CREATED");

		String actionLogId = webTestClient.get()
				.uri("/api/operator/incidents/{incidentId}/review", incidentId)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.actionLogs[0].id").value(String.class, value -> assertThat(value).isNotBlank())
				.returnResult()
				.getResponseBodyContent() == null ? null : null;

		EntityExchangeResult<byte[]> actionLogSummary = webTestClient.get()
				.uri("/api/operator/incidents/{incidentId}/review", incidentId)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult();

		String extractedActionLogId = com.jayway.jsonpath.JsonPath.read(
				new String(actionLogSummary.getResponseBodyContent()),
				"$.actionLogs[0].id"
		);

		webTestClient.post()
				.uri("/api/operator/action-logs/{actionLogId}/approve", extractedActionLogId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "reason": "approved by operator"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("APPROVED_BY_HUMAN");

		webTestClient.post()
				.uri("/api/operator/action-logs/{actionLogId}/outcome", extractedActionLogId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "outcomeStatus": "NOT_EFFECTIVE",
						  "outcomeSummary": "action was not effective",
						  "observedSignals": ["payment.error.rate increased"]
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("POSTMORTEM_REQUIRED")
				.jsonPath("$.postmortemRequired").isEqualTo(true);

		webTestClient.get()
				.uri("/api/postmortem-drafts/incidents/{incidentId}", incidentId)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.incidentId").isEqualTo(incidentId)
				.jsonPath("$.requiresHumanReview").isEqualTo(true);

		EntityExchangeResult<byte[]> improvementResult = webTestClient.post()
				.uri("/api/improvement-candidates/incidents/{incidentId}/generate", incidentId)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult();

		List<ImprovementCandidateResponse> candidates = objectMapper.readValue(
				improvementResult.getResponseBody(),
				new TypeReference<>() {
				}
		);
		assertThat(candidates).isNotEmpty();

		String candidateId = candidates.get(0).id();

		webTestClient.post()
				.uri("/api/improvement-candidates/{candidateId}/accept", candidateId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "reason": "accepted for review"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("ACCEPTED_BY_HUMAN");

		EntityExchangeResult<byte[]> knowledgeResult = webTestClient.post()
				.uri("/api/knowledge-update-reviews/improvement-candidates/{candidateId}/create", candidateId)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult();

		List<KnowledgeUpdateReviewResponse> reviews = objectMapper.readValue(
				knowledgeResult.getResponseBody(),
				new TypeReference<>() {
				}
		);
		assertThat(reviews).isNotEmpty();
		assertThat(reviews.get(0).status().name()).isEqualTo("REQUESTED");

		webTestClient.get()
				.uri("/api/incidents/{incidentId}/lifecycle", incidentId)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("KNOWLEDGE_REVIEW_REQUESTED");
	}
}
