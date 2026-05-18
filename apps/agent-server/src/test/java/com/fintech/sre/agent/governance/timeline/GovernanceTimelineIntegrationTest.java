package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.sre.agent.governance.detail.GovernanceDetailSanitizer;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStore;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;

class GovernanceTimelineIntegrationTest {

	private WebTestClient webTestClient;
	private ObjectMapper objectMapper;
	private SimpleMeterRegistry meterRegistry;

	private RecommendationRecordStore recommendationRecordStore;
	private RecommendationApprovalStore recommendationApprovalStore;
	private ExecutionPlanStore executionPlanStore;
	private HumanExecutionResultStore humanExecutionResultStore;
	private VerificationResultStore verificationResultStore;
	private IncidentLifecycleStore incidentLifecycleStore;
	private PostmortemDraftStore postmortemDraftStore;
	private PostmortemReviewStore postmortemReviewStore;
	private LearningCandidateStore learningCandidateStore;
	private KnowledgePromotionReviewStore knowledgePromotionReviewStore;
	private KnowledgePromotionPlanStore knowledgePromotionPlanStore;
	private KnowledgeUpdateApplicationStore knowledgeUpdateApplicationStore;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

		recommendationRecordStore = mock(RecommendationRecordStore.class);
		recommendationApprovalStore = mock(RecommendationApprovalStore.class);
		executionPlanStore = mock(ExecutionPlanStore.class);
		humanExecutionResultStore = mock(HumanExecutionResultStore.class);
		verificationResultStore = mock(VerificationResultStore.class);
		incidentLifecycleStore = mock(IncidentLifecycleStore.class);
		postmortemDraftStore = mock(PostmortemDraftStore.class);
		postmortemReviewStore = mock(PostmortemReviewStore.class);
		learningCandidateStore = mock(LearningCandidateStore.class);
		knowledgePromotionReviewStore = mock(KnowledgePromotionReviewStore.class);
		knowledgePromotionPlanStore = mock(KnowledgePromotionPlanStore.class);
		knowledgeUpdateApplicationStore = mock(KnowledgeUpdateApplicationStore.class);

		seedDefaultStoreResponses();

		meterRegistry = new SimpleMeterRegistry();
		GovernanceTimelineMetricsRecorder metricsRecorder =
				new GovernanceTimelineMetricsRecorder(
						new GovernanceMetricsRecorder(meterRegistry),
						meterRegistry
				);
		DefaultGovernanceTimelineAggregationService aggregationService =
				new DefaultGovernanceTimelineAggregationService(
						recommendationRecordStore,
						recommendationApprovalStore,
						executionPlanStore,
						humanExecutionResultStore,
						verificationResultStore,
						incidentLifecycleStore,
						postmortemDraftStore,
						postmortemReviewStore,
						learningCandidateStore,
						knowledgePromotionReviewStore,
						knowledgePromotionPlanStore,
						knowledgeUpdateApplicationStore,
						new DefaultGovernanceTimelineProjectionMapper(
								new GovernanceDetailSanitizer()
						),
						new DefaultGovernanceTimelineCursorCodec(objectMapper),
						metricsRecorder
				);

		webTestClient = WebTestClient.bindToController(
				new GovernanceTimelineController(
						aggregationService,
						new GovernanceTimelineQueryParser(),
						metricsRecorder
				)
		).configureClient().build();
	}

	@Test
	void shouldQueryTimelineThroughFullApiFlow() throws Exception {
		JsonNode body = responseJson("/internal/governance/timeline?limit=10");

		assertThat(body.path("status").asText()).isEqualTo("SUCCESS");
		assertThat(body.path("page").path("items")).hasSize(6);
		assertThat(body.path("page").path("items").get(0).path("recordId").asText())
				.isEqualTo("learning-1");
		assertThat(body.path("page").path("items").get(1).path("recordId").asText())
				.isEqualTo("lifecycle-1b");
		assertThat(body.path("page").path("items").get(2).path("recordId").asText())
				.isEqualTo("approval-1");
		assertThat(body.path("page").path("items").get(3).path("recordId").asText())
				.isEqualTo("rec-2");
		assertThat(body.path("page").path("items").get(4).path("recordId").asText())
				.isEqualTo("rec-1");
		assertThat(body.path("page").path("page").path("nextCursor").asText()).isNotBlank();
		assertThat(body.path("page").path("page").path("previousCursor").asText()).isNotBlank();
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "success")
				.tag("direction", "NEXT")
				.counter()
				.count()).isEqualTo(1.0);
		assertNoHighCardinalityTags(GovernanceTimelineMetricName.QUERY_TOTAL);
		assertNoHighCardinalityTags(GovernanceTimelineMetricName.PAGE_SIZE);
	}

	@Test
	void shouldReturnOlderEventsForNextPagination() throws Exception {
		JsonNode firstPage = responseJson("/internal/governance/timeline?limit=3");
		String nextCursor = firstPage.path("page").path("page").path("nextCursor").asText();

		JsonNode nextPage = responseJson(
				"/internal/governance/timeline?cursor=%s&direction=NEXT&limit=3"
						.formatted(nextCursor)
		);

		assertThat(nextPage.path("status").asText()).isEqualTo("SUCCESS");
		assertThat(nextPage.path("page").path("items")).hasSize(3);
		assertThat(nextPage.path("page").path("items").get(0).path("recordId").asText())
				.isEqualTo("rec-2");
		assertThat(nextPage.path("page").path("items").get(1).path("recordId").asText())
				.isEqualTo("rec-1");
		assertThat(nextPage.path("page").path("items").get(2).path("recordId").asText())
				.isEqualTo("lifecycle-1a");
		assertThat(recordIds(nextPage)).doesNotHaveDuplicates();
	}

	@Test
	void shouldReturnNewerEventsForPreviousPagination() throws Exception {
		JsonNode firstPage = responseJson("/internal/governance/timeline?limit=3");
		String nextCursor = firstPage.path("page").path("page").path("nextCursor").asText();
		JsonNode olderPage = responseJson(
				"/internal/governance/timeline?cursor=%s&direction=NEXT&limit=3"
						.formatted(nextCursor)
		);
		String previousCursor = olderPage.path("page").path("page").path("previousCursor")
				.asText();

		JsonNode previousPage = responseJson(
				"/internal/governance/timeline?cursor=%s&direction=PREVIOUS&limit=3"
						.formatted(previousCursor)
		);

		assertThat(previousPage.path("status").asText()).isEqualTo("SUCCESS");
		assertThat(previousPage.path("page").path("items")).hasSize(3);
		assertThat(previousPage.path("page").path("items").get(0).path("recordId").asText())
				.isEqualTo("learning-1");
		assertThat(previousPage.path("page").path("items").get(1).path("recordId").asText())
				.isEqualTo("lifecycle-1b");
		assertThat(previousPage.path("page").path("items").get(2).path("recordId").asText())
				.isEqualTo("approval-1");
		assertThat(previousPage.path("page").path("page").path("hasNext").asBoolean())
				.isTrue();
		assertThat(previousPage.path("page").path("page").path("hasPrevious").asBoolean())
				.isFalse();
	}

	@Test
	void shouldApplyEventTypeFilter() throws Exception {
		JsonNode body = responseJson(
				"/internal/governance/timeline?eventType=INCIDENT_TRANSITIONED"
		);

		assertThat(body.path("status").asText()).isEqualTo("SUCCESS");
		assertThat(body.path("page").path("items")).hasSize(2);
		for (JsonNode item : body.path("page").path("items")) {
			assertThat(item.path("type").asText()).isEqualTo("INCIDENT_TRANSITIONED");
		}
	}

	@Test
	void shouldReturnBadRequestForInvalidCursor() throws Exception {
		JsonNode body = responseJsonBadRequest(
				"/internal/governance/timeline?cursor=invalid"
		);

		assertThat(body.path("status").asText()).isEqualTo("FAILURE");
		assertThat(body.path("page").isMissingNode() || body.path("page").isNull()).isTrue();
		assertThat(body.path("degradation").path("degraded").asBoolean()).isFalse();
		assertThat(body.path("errors").get(0).path("code").asText())
				.isEqualTo("INVALID_TIMELINE_CURSOR");
		assertThat(body.path("errors").get(0).path("message").asText())
				.isEqualTo("Invalid timeline cursor.");
		assertThat(body.toString()).doesNotContain("cursor=invalid");
		assertThat(body.toString()).doesNotContain("Illegal base64");
		assertThat(body.toString()).doesNotContain("JsonParseException");
		assertThat(body.toString()).doesNotContain("stackTrace");
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "invalid_cursor")
				.tag("direction", "NEXT")
				.counter()
				.count()).isEqualTo(1.0);
		assertNoHighCardinalityTags(GovernanceTimelineMetricName.QUERY_TOTAL);
	}

	@Test
	void shouldReturnBadRequestForInvalidQuery() throws Exception {
		JsonNode body = responseJsonBadRequest(
				"/internal/governance/timeline?from=2026-05-14T02:00:00Z"
		);

		assertThat(body.path("status").asText()).isEqualTo("FAILURE");
		assertThat(body.path("page").isMissingNode() || body.path("page").isNull()).isTrue();
		assertThat(body.path("degradation").path("degraded").asBoolean()).isFalse();
		assertThat(body.path("errors").get(0).path("code").asText())
				.isEqualTo("INVALID_TIMELINE_QUERY");
		assertThat(body.path("errors").get(0).path("message").asText())
				.isEqualTo("Invalid timeline query.");
		assertThat(body.toString()).doesNotContain("DateTimeParseException");
		assertThat(body.toString()).doesNotContain("IllegalArgumentException");
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "invalid_query")
				.tag("direction", "unknown")
				.counter()
				.count()).isEqualTo(1.0);
		assertNoHighCardinalityTags(GovernanceTimelineMetricName.QUERY_TOTAL);
	}

	@Test
	void shouldReturnBadRequestWhenFromIsAfterTo() throws Exception {
		JsonNode body = responseJsonBadRequest(
				"/internal/governance/timeline?from=2026-05-14T05:00:00Z&to=2026-05-14T01:00:00Z"
		);

		assertThat(body.path("status").asText()).isEqualTo("FAILURE");
		assertThat(body.path("errors").get(0).path("code").asText())
				.isEqualTo("INVALID_TIMELINE_QUERY");
	}

	@Test
	void shouldReturnBadRequestForInvalidDirection() throws Exception {
		JsonNode body = responseJsonBadRequest(
				"/internal/governance/timeline?direction=INVALID"
		);

		assertThat(body.path("status").asText()).isEqualTo("FAILURE");
		assertThat(body.path("page").isMissingNode() || body.path("page").isNull()).isTrue();
		assertThat(body.path("degradation").path("degraded").asBoolean()).isFalse();
		assertThat(body.path("errors").get(0).path("code").asText())
				.isEqualTo("INVALID_TIMELINE_QUERY");
		assertThat(body.path("errors").get(0).path("message").asText())
				.isEqualTo("Invalid timeline query.");
		assertThat(body.toString()).doesNotContain("No enum constant");
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "invalid_query")
				.tag("direction", "unknown")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnBadRequestForInvalidEventType() throws Exception {
		JsonNode body = responseJsonBadRequest(
				"/internal/governance/timeline?eventType=INVALID_EVENT"
		);

		assertThat(body.path("status").asText()).isEqualTo("FAILURE");
		assertThat(body.path("page").isMissingNode() || body.path("page").isNull()).isTrue();
		assertThat(body.path("degradation").path("degraded").asBoolean()).isFalse();
		assertThat(body.path("errors").get(0).path("code").asText())
				.isEqualTo("INVALID_TIMELINE_QUERY");
		assertThat(body.path("errors").get(0).path("message").asText())
				.isEqualTo("Invalid timeline query.");
		assertThat(body.toString()).doesNotContain("No enum constant");
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "invalid_query")
				.tag("direction", "unknown")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnDegradedTimelineWhenOneStoreFails() throws Exception {
		when(verificationResultStore.findRecent(anyInt()))
				.thenReturn(Flux.error(new IllegalStateException("boom")));

		JsonNode body = responseJson("/internal/governance/timeline?limit=10");

		assertThat(body.path("status").asText()).isEqualTo("DEGRADED");
		assertThat(body.path("page").path("items")).isNotEmpty();
		assertThat(body.path("degradation").path("degraded").asBoolean()).isTrue();
		assertThat(body.path("degradation").path("failedComponents")).hasSize(1);
		assertThat(body.path("degradation").path("failedComponents").get(0).path("source")
				.asText()).isEqualTo("VERIFICATION");
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "degraded")
				.tag("direction", "NEXT")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.DEGRADED_TOTAL)
				.tag(GovernanceTimelineMetricTag.SOURCE, "VERIFICATION")
				.counter()
				.count()).isEqualTo(1.0);
		assertNoHighCardinalityTags(GovernanceTimelineMetricName.DEGRADED_TOTAL);
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.PAGE_SIZE)
				.tag(GovernanceTimelineMetricTag.MODE, "PARTIAL_DEGRADED")
				.summary()
				.count()).isEqualTo(1L);
	}

	@Test
	void shouldReturnSafeInternalServerErrorForUnexpectedAggregationFailure() throws Exception {
		when(recommendationRecordStore.findRecent(anyInt()))
				.thenThrow(new RuntimeException("sensitive DB detail"));

		byte[] responseBody = webTestClient.get()
				.uri("/internal/governance/timeline?limit=10")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().is5xxServerError()
				.expectBody()
				.returnResult()
				.getResponseBody();

		JsonNode body = objectMapper.readTree(responseBody);

		assertThat(body.path("status").asText()).isEqualTo("FAILURE");
		assertThat(body.path("page").isMissingNode() || body.path("page").isNull()).isTrue();
		assertThat(body.path("degradation").path("degraded").asBoolean()).isFalse();
		assertThat(body.path("errors").get(0).path("code").asText())
				.isEqualTo("TIMELINE_QUERY_FAILED");
		assertThat(body.path("errors").get(0).path("message").asText())
				.isEqualTo("Timeline query failed.");
		assertThat(body.toString()).doesNotContain("sensitive DB detail");
		assertThat(body.toString()).doesNotContain("SQL");
		assertThat(body.toString()).doesNotContain("RuntimeException");
		assertThat(body.toString()).doesNotContain("stackTrace");
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "failure")
				.tag("direction", "NEXT")
				.counter()
				.count()).isEqualTo(1.0);
		assertNoHighCardinalityTags(GovernanceTimelineMetricName.QUERY_TOTAL);
	}

	@Test
	void shouldRecordEmptyQueryMetric() throws Exception {
		when(recommendationRecordStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(recommendationApprovalStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(incidentLifecycleStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(learningCandidateStore.findRecent(anyInt())).thenReturn(Flux.empty());

		JsonNode body = responseJson("/internal/governance/timeline?limit=10");

		assertThat(body.path("status").asText()).isEqualTo("SUCCESS");
		assertThat(body.path("page").path("items")).isEmpty();
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "empty")
				.tag("direction", "NEXT")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(meterRegistry.get(GovernanceTimelineMetricName.PAGE_SIZE)
				.tag(GovernanceTimelineMetricTag.MODE, "STRICT")
				.summary()
				.count()).isEqualTo(1L);
		assertNoHighCardinalityTags(GovernanceTimelineMetricName.QUERY_TOTAL);
	}

	@Test
	void shouldRecordPageSizeSummaryMetricForSuccessfulQuery() throws Exception {
		responseJson("/internal/governance/timeline?limit=10");

		assertThat(meterRegistry.get(GovernanceTimelineMetricName.PAGE_SIZE)
				.tag(GovernanceTimelineMetricTag.MODE, "STRICT")
				.summary()
				.totalAmount()).isEqualTo(6.0);
		assertNoHighCardinalityTags(GovernanceTimelineMetricName.PAGE_SIZE);
	}

	@Test
	void shouldKeepStableTieBreakerOrderingInApiResponse() throws Exception {
		JsonNode body = responseJson("/internal/governance/timeline?limit=10");

		assertThat(body.path("page").path("items").get(3).path("occurredAt").asText())
				.isEqualTo("2026-05-14T02:00:00Z");
		assertThat(body.path("page").path("items").get(4).path("occurredAt").asText())
				.isEqualTo("2026-05-14T02:00:00Z");
		assertThat(body.path("page").path("items").get(3).path("recordId").asText())
				.isEqualTo("rec-2");
		assertThat(body.path("page").path("items").get(4).path("recordId").asText())
				.isEqualTo("rec-1");
	}

	private void seedDefaultStoreResponses() {
		when(recommendationRecordStore.findRecent(anyInt())).thenReturn(Flux.just(
				recommendationOne(),
				recommendationTwo()
		));
		when(recommendationApprovalStore.findRecent(anyInt())).thenReturn(Flux.just(
				approvalOne()
		));
		when(executionPlanStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(humanExecutionResultStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(verificationResultStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(incidentLifecycleStore.findRecent(anyInt())).thenReturn(Flux.just(
				lifecycleOne(),
				lifecycleTwo()
		));
		when(postmortemDraftStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(postmortemReviewStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(learningCandidateStore.findRecent(anyInt())).thenReturn(Flux.just(
				learningOne()
		));
		when(knowledgePromotionReviewStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(knowledgePromotionPlanStore.findRecent(anyInt())).thenReturn(Flux.empty());
		when(knowledgeUpdateApplicationStore.findRecent(anyInt())).thenReturn(Flux.empty());
	}

	private JsonNode responseJson(String uri) throws Exception {
		byte[] responseBody = webTestClient.get()
				.uri(uri)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult()
				.getResponseBody();

		return objectMapper.readTree(responseBody);
	}

	private JsonNode responseJsonBadRequest(String uri) throws Exception {
		byte[] responseBody = webTestClient.get()
				.uri(uri)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.returnResult()
				.getResponseBody();

		return objectMapper.readTree(responseBody);
	}

	private List<String> recordIds(JsonNode body) {
		return java.util.stream.StreamSupport
				.stream(body.path("page").path("items").spliterator(), false)
				.map(item -> item.path("recordId").asText())
				.toList();
	}

	private void assertNoHighCardinalityTags(String metricName) {
		for (Meter meter : meterRegistry.find(metricName).meters()) {
			assertThat(meter.getId().getTags()).allSatisfy(tag -> {
				String key = tag.getKey().toLowerCase();
				assertThat(key).doesNotContain("cursor");
				assertThat(key).doesNotContain("eventid");
				assertThat(key).doesNotContain("recordid");
				assertThat(key).doesNotContain("incidentid");
				assertThat(key).doesNotContain("query");
				assertThat(key).doesNotContain("exception");
				assertThat(key).doesNotContain("summary");
				assertThat(key).doesNotContain("message");
			});
		}
	}

	private RecommendationRecord recommendationOne() {
		return new RecommendationRecord(
				"rec-1",
				"incident-1",
				"audit-1",
				"ai",
				"svc-a",
				"payments",
				"HIGH",
				"CREATED",
				Instant.parse("2026-05-14T02:00:00Z"),
				1,
				0,
				"ALLOW",
				"ALLOW",
				List.of("restart"),
				List.of(),
				Map.of()
		);
	}

	private RecommendationRecord recommendationTwo() {
		return new RecommendationRecord(
				"rec-2",
				"incident-1",
				"audit-2",
				"ai",
				"svc-b",
				"payments",
				"MEDIUM",
				"CREATED",
				Instant.parse("2026-05-14T02:00:00Z"),
				1,
				0,
				"ALLOW",
				"ALLOW",
				List.of("rollback"),
				List.of(),
				Map.of()
		);
	}

	private RecommendationApprovalRecord approvalOne() {
		return new RecommendationApprovalRecord(
				"approval-1",
				"rec-1",
				"incident-1",
				RecommendationApprovalStatus.APPROVED,
				"operator-1",
				"approved",
				Instant.parse("2026-05-14T03:00:00Z"),
				Map.of()
		);
	}

	private IncidentLifecycleRecord lifecycleOne() {
		return new IncidentLifecycleRecord(
				"lifecycle-1a",
				"incident-1",
				IncidentStatus.OPEN,
				IncidentStatus.MITIGATING,
				IncidentTransitionReason.MITIGATION_IN_PROGRESS,
				"operator-1",
				"Mitigating",
				Instant.parse("2026-05-14T01:00:00Z"),
				Map.of()
		);
	}

	private IncidentLifecycleRecord lifecycleTwo() {
		return new IncidentLifecycleRecord(
				"lifecycle-1b",
				"incident-1",
				IncidentStatus.MITIGATING,
				IncidentStatus.RESOLVED,
				IncidentTransitionReason.INCIDENT_RESOLVED,
				"operator-1",
				"Resolved",
				Instant.parse("2026-05-14T04:00:00Z"),
				Map.of()
		);
	}

	private LearningCandidateRecord learningOne() {
		return new LearningCandidateRecord(
				"learning-1",
				"incident-1",
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.APPROVED,
				"system",
				"Runbook update candidate",
				List.of("update runbook"),
				Instant.parse("2026-05-14T05:00:00Z"),
				Map.of()
		);
	}
}
