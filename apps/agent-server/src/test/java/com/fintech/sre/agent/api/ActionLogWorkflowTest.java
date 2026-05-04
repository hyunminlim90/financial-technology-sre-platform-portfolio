package com.fintech.sre.agent.api;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fintech.sre.agent.actionlog.entity.ExecutedActionEntity;
import com.fintech.sre.agent.actionlog.entity.IncidentRecommendationEntity;
import com.fintech.sre.agent.actionlog.entity.RecommendationActionEntity;
import com.fintech.sre.agent.actionlog.repository.IncidentRecommendationRepository;
import com.fintech.sre.agent.actionlog.repository.RecommendationActionRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActionLogWorkflowTest {

	private final ApplicationContext applicationContext;
	private WebTestClient webTestClient;

	ActionLogWorkflowTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
	}

	@Test
	void actionLogsFeedPostmortemGenerationWhenManualInputsAreMissing() {
		String incidentId = "INC-4001";

		webTestClient.post()
				.uri("/api/v1/incidents/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-4001",
						  "alertName": "RedisLatencyHigh",
						  "service": "payment-api",
						  "environment": "prod",
						  "severityHint": "SEV_2",
						  "occurredAt": "2026-05-02T02:00:00Z",
						  "metricsSnapshot": {
						    "p95LatencyMs": 520.0,
						    "errorRate": 0.08,
						    "retryRate": 0.05,
						    "redisTimeoutCount": 11,
						    "kafkaConsumerLag": 5000
						  },
						  "logsSample": [
						    {
						      "timestamp": "2026-05-02T02:00:05Z",
						      "level": "ERROR",
						      "message": "Redis timeout detected in payment path",
						      "traceId": "trace-401"
						    }
						  ],
						  "traceIds": ["trace-401"],
						  "operatorNote": "Retry amplification observed during Redis degradation"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("RECOMMENDATION_CREATED");

		List<IncidentRecommendationEntity> recommendations = incidentRecommendationRepository()
				.findByIncidentId(incidentId);
		List<RecommendationActionEntity> recommendationActions = recommendationActionRepository()
				.findByIncidentId(incidentId);

		org.junit.jupiter.api.Assertions.assertFalse(recommendations.isEmpty());
		org.junit.jupiter.api.Assertions.assertFalse(recommendationActions.isEmpty());

		String recommendationId = recommendations.get(recommendations.size() - 1).recommendationId();
		Long recommendationActionId = recommendationActions.get(0).id();

		EntityExchangeResult<ExecutedActionEntity> executedActionResult = webTestClient.post()
				.uri("/api/v1/incidents/{incidentId}/actions", incidentId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "recommendationId": "%s",
						  "recommendationActionId": %d,
						  "action": "scale-out payment-api",
						  "executedBy": "sre-operator",
						  "executedAt": "2026-05-02T02:07:00Z",
						  "executionMethod": "manual",
						  "executionDetail": "Increased replicas from 6 to 9",
						  "expectedEffect": "Reduce queue pressure",
						  "actualEffect": "DB pending increased further",
						  "rollbackPlan": "Restore replicas from 9 to 6"
						}
						""".formatted(recommendationId, recommendationActionId))
				.exchange()
				.expectStatus().isOk()
				.expectBody(ExecutedActionEntity.class)
				.returnResult();

		Long executedActionId = executedActionResult.getResponseBody().id();

		webTestClient.post()
				.uri("/api/v1/incidents/{incidentId}/actions/{actionId}/verification", incidentId, executedActionId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "metricName": "retry_rate",
						  "query": "sum(rate(payment_retry_total[5m])) / sum(rate(payment_request_total[5m]))",
						  "beforeValue": 0.24,
						  "afterValue": 0.21,
						  "expectedCondition": "retry_rate < 0.1",
						  "status": "FAILED"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("FAILED");

		webTestClient.post()
				.uri("/api/v1/incidents/{incidentId}/actions/{actionId}/rollback", incidentId, executedActionId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "rollbackAction": "Restore replicas from 9 to 6",
						  "rollbackReason": "DB pending increased after scale-out",
						  "rollbackBy": "sre-operator",
						  "rollbackAt": "2026-05-02T02:12:00Z",
						  "verificationStatus": "PASSED"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.verificationStatus").isEqualTo("PASSED");

		webTestClient.post()
				.uri("/api/v1/postmortems/generate")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "incidentId": "INC-4001",
						  "operatorSummary": "Incident stabilized after rollback"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("DRAFT_CREATED")
				.jsonPath("$.draft.timeline.length()").isEqualTo(4)
				.jsonPath("$.draft.whatWentWrong[0]").value(value -> org.assertj.core.api.Assertions.assertThat((String) value)
						.contains("Action: scale-out payment-api"))
				.jsonPath("$.improvementCandidates.length()").isEqualTo(1)
				.jsonPath("$.preventiveDesignCandidates.length()").isEqualTo(1)
				.jsonPath("$.warnings.length()").isEqualTo(2);
	}

	private IncidentRecommendationRepository incidentRecommendationRepository() {
		return applicationContext.getBean(IncidentRecommendationRepository.class);
	}

	private RecommendationActionRepository recommendationActionRepository() {
		return applicationContext.getBean(RecommendationActionRepository.class);
	}
}
