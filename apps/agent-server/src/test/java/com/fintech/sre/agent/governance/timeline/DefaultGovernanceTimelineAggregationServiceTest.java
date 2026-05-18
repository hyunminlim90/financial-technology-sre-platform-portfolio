package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.detail.GovernanceDetailSanitizer;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;
import com.fintech.sre.agent.incident.lifecycle.InMemoryIncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.learning.application.InMemoryKnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.candidate.InMemoryLearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.plan.InMemoryKnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.promotion.InMemoryKnowledgePromotionReviewStore;
import com.fintech.sre.agent.postmortem.draft.InMemoryPostmortemDraftStore;
import com.fintech.sre.agent.postmortem.review.InMemoryPostmortemReviewStore;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.execution.InMemoryExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.result.InMemoryHumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.verification.InMemoryVerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;

class DefaultGovernanceTimelineAggregationServiceTest {

	private final GovernanceTimelineCursorCodec cursorCodec =
			new DefaultGovernanceTimelineCursorCodec(objectMapper());
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final GovernanceTimelineMetricsRecorder metricsRecorder =
			new GovernanceTimelineMetricsRecorder(
					new GovernanceMetricsRecorder(meterRegistry),
					meterRegistry
			);

	@Test
	void shouldMergeAndSortMultipleSourceProjections() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(10, null, List.of())
		).block();

		assertThat(result.degraded()).isFalse();
		assertThat(result.page().items()).extracting(item -> item.type() + ":" + item.recordId())
				.containsExactly(
						"LEARNING_CANDIDATE_CREATED:learning-1",
						"INCIDENT_TRANSITIONED:lifecycle-1b",
						"APPROVAL_DECIDED:approval-1",
						"RECOMMENDATION_CREATED:rec-2",
						"RECOMMENDATION_CREATED:rec-1",
						"INCIDENT_TRANSITIONED:lifecycle-1a"
				);
	}

	@Test
	void shouldDeduplicateByEventId() {
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore() {
					@Override
					public Flux<RecommendationRecord> findRecent(int limit) {
						RecommendationRecord record = recommendationOne();
						return Flux.just(record, record);
					}
				};
		DefaultGovernanceTimelineAggregationService service = service(
				recommendationStore,
				new InMemoryRecommendationApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new InMemoryIncidentLifecycleStore(),
				new InMemoryPostmortemDraftStore(),
				new InMemoryPostmortemReviewStore(),
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgePromotionReviewStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgeUpdateApplicationStore()
		);

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(10, null, List.of(GovernanceTimelineAggregationSource.RECOMMENDATION))
		).block();

		assertThat(result.page().items()).singleElement().satisfies(item ->
				assertThat(item.recordId()).isEqualTo("rec-1"));
	}

	@Test
	void shouldApplyLimit() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(2, null, List.of())
		).block();

		assertThat(result.page().items()).hasSize(2);
		assertThat(result.page().page().limit()).isEqualTo(2);
	}

	@Test
	void shouldApplyEventTypeFilter() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();
		GovernanceTimelineFilter filter = new GovernanceTimelineFilter(
				null,
				null,
				null,
				null,
				null,
				null,
				List.of(GovernanceTimelineEventType.RECOMMENDATION_CREATED),
				false
		);

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(10, filter, List.of())
		).block();

		assertThat(result.page().items()).extracting(item -> item.type())
				.containsOnly("RECOMMENDATION_CREATED");
	}

	@Test
	void shouldApplySourceFilter() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(
						10,
						null,
						List.of(
								GovernanceTimelineAggregationSource.INCIDENT_LIFECYCLE,
								GovernanceTimelineAggregationSource.RECOMMENDATION
						)
				)
		).block();

		assertThat(result.page().items()).extracting(item -> item.type())
				.containsOnly(
						"INCIDENT_TRANSITIONED",
						"RECOMMENDATION_CREATED"
				);
	}

	@Test
	void shouldApplyTimeRangeFilter() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();
		GovernanceTimelineFilter filter = new GovernanceTimelineFilter(
				null,
				null,
				null,
				null,
				Instant.parse("2026-05-14T02:00:00Z"),
				Instant.parse("2026-05-14T04:00:00Z"),
				List.of(),
				false
		);

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(10, filter, List.of())
		).block();

		assertThat(result.page().items()).extracting(item -> item.recordId())
				.containsExactly("lifecycle-1b", "approval-1", "rec-2", "rec-1");
	}

	@Test
	void shouldReturnDegradedResultWhenStoreFails() {
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		InMemoryIncidentLifecycleStore incidentStore =
				new InMemoryIncidentLifecycleStore();
		InMemoryLearningCandidateStore learningStore =
				new InMemoryLearningCandidateStore();
		recommendationStore.save(recommendationOne()).block();
		recommendationStore.save(recommendationTwo()).block();
		approvalStore.save(approvalOne()).block();
		incidentStore.save(lifecycleOne()).block();
		learningStore.save(learningOne()).block();

		DefaultGovernanceTimelineAggregationService service = service(
				recommendationStore,
				approvalStore,
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore() {
					@Override
					public Flux<VerificationResultRecord> findRecent(int limit) {
						return Flux.error(new IllegalStateException("boom"));
					}
				},
				incidentStore,
				new InMemoryPostmortemDraftStore(),
				new InMemoryPostmortemReviewStore(),
				learningStore,
				new InMemoryKnowledgePromotionReviewStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgeUpdateApplicationStore()
		);

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(10, null, List.of())
		).block();

		assertThat(result.degraded()).isTrue();
		assertThat(result.failedSources()).containsExactly("VERIFICATION");
		assertThat(result.reason()).isEqualTo("timeline_aggregation_degraded");
		assertThat(result.page().page().degraded()).isTrue();
		assertThat(result.page().page().failedComponents()).containsExactly("VERIFICATION");
		assertThat(meterRegistry.find(GovernanceTimelineMetricName.DEGRADED_TOTAL)
				.tag(GovernanceTimelineMetricTag.SOURCE, "VERIFICATION")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldExposeNullCursorMetadataForInMemoryPhase() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(10, null, List.of())
		).block();

		assertThat(result.page().page().hasNext()).isFalse();
		assertThat(result.page().page().hasPrevious()).isFalse();
		assertThat(result.page().page().ordering())
				.isEqualTo("occurredAt DESC, eventId DESC");
		assertThat(result.page().page().previousCursor()).isNotBlank();
		assertThat(result.page().page().nextCursor()).isNotBlank();
		assertThat(cursorCodec.decode(result.page().page().previousCursor()))
				.isEqualTo(new GovernanceTimelineCursor(
						Instant.parse("2026-05-14T05:00:00Z"),
						"LEARNING_CANDIDATE_CREATED",
						"LEARNING_CANDIDATE:learning-1"
				));
		assertThat(cursorCodec.decode(result.page().page().nextCursor()))
				.isEqualTo(new GovernanceTimelineCursor(
						Instant.parse("2026-05-14T01:00:00Z"),
						"INCIDENT_TRANSITIONED",
						"INCIDENT_LIFECYCLE:lifecycle-1a"
				));
	}

	@Test
	void shouldKeepCursorsNullForEmptyPage() {
		DefaultGovernanceTimelineAggregationService service = service(
				new InMemoryRecommendationRecordStore(),
				new InMemoryRecommendationApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new InMemoryIncidentLifecycleStore(),
				new InMemoryPostmortemDraftStore(),
				new InMemoryPostmortemReviewStore(),
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgePromotionReviewStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgeUpdateApplicationStore()
		);

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(10, null, List.of())
		).block();

		assertThat(result.page().items()).isEmpty();
		assertThat(result.page().page().previousCursor()).isNull();
		assertThat(result.page().page().nextCursor()).isNull();
	}

	@Test
	void shouldReturnOlderEventsForNextCursor() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();
		String cursor = cursorCodec.encode(new GovernanceTimelineCursor(
				Instant.parse("2026-05-14T03:00:00Z"),
				"APPROVAL_DECIDED",
				"APPROVAL_RECORD:approval-1"
		));

		GovernanceTimelineAggregationResult result = service.aggregate(
				new GovernanceTimelineAggregationRequest(
						new GovernanceTimelineQuery(
								cursor,
								10,
								GovernanceCursorDirection.NEXT,
								null
						),
						List.of()
				)
		).block();

		assertThat(result.page().items()).extracting(item -> item.recordId())
				.containsExactly("rec-2", "rec-1", "lifecycle-1a");
		assertThat(result.page().page().hasNext()).isFalse();
		assertThat(result.page().page().hasPrevious()).isTrue();
	}

	@Test
	void shouldReturnNewerEventsForPreviousCursor() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();
		String cursor = cursorCodec.encode(new GovernanceTimelineCursor(
				Instant.parse("2026-05-14T02:00:00Z"),
				"RECOMMENDATION_CREATED",
				"RECOMMENDATION_RECORD:rec-1"
		));

		GovernanceTimelineAggregationResult result = service.aggregate(
				new GovernanceTimelineAggregationRequest(
						new GovernanceTimelineQuery(
								cursor,
								10,
								GovernanceCursorDirection.PREVIOUS,
								null
						),
						List.of()
				)
		).block();

		assertThat(result.page().items()).extracting(item -> item.recordId())
				.containsExactly("learning-1", "lifecycle-1b", "approval-1", "rec-2");
		assertThat(result.page().page().hasNext()).isTrue();
		assertThat(result.page().page().hasPrevious()).isFalse();
	}

	@Test
	void shouldUseEventIdTieBreakerForNextCursorWhenTimestampMatches() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();
		String cursor = cursorCodec.encode(new GovernanceTimelineCursor(
				Instant.parse("2026-05-14T02:00:00Z"),
				"RECOMMENDATION_CREATED",
				"RECOMMENDATION_RECORD:rec-2"
		));

		GovernanceTimelineAggregationResult result = service.aggregate(
				new GovernanceTimelineAggregationRequest(
						new GovernanceTimelineQuery(
								cursor,
								10,
								GovernanceCursorDirection.NEXT,
								null
						),
						List.of()
				)
		).block();

		assertThat(result.page().items()).extracting(item -> item.recordId())
				.startsWith("rec-1");
	}

	@Test
	void shouldUseEventIdTieBreakerForPreviousCursorWhenTimestampMatches() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();
		String cursor = cursorCodec.encode(new GovernanceTimelineCursor(
				Instant.parse("2026-05-14T02:00:00Z"),
				"RECOMMENDATION_CREATED",
				"RECOMMENDATION_RECORD:rec-1"
		));

		GovernanceTimelineAggregationResult result = service.aggregate(
				new GovernanceTimelineAggregationRequest(
						new GovernanceTimelineQuery(
								cursor,
								10,
								GovernanceCursorDirection.PREVIOUS,
								null
						),
						List.of()
				)
		).block();

		assertThat(result.page().items()).extracting(item -> item.recordId())
				.contains("rec-2");
	}

	@Test
	void shouldSetHasNextWhenOlderPageExists() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();
		String cursor = cursorCodec.encode(new GovernanceTimelineCursor(
				Instant.parse("2026-05-14T05:00:00Z"),
				"LEARNING_CANDIDATE_CREATED",
				"LEARNING_CANDIDATE:learning-1"
		));

		GovernanceTimelineAggregationResult result = service.aggregate(
				new GovernanceTimelineAggregationRequest(
						new GovernanceTimelineQuery(
								cursor,
								2,
								GovernanceCursorDirection.NEXT,
								null
						),
						List.of()
				)
		).block();

		assertThat(result.page().items()).hasSize(2);
		assertThat(result.page().page().hasNext()).isTrue();
		assertThat(result.page().page().hasPrevious()).isTrue();
	}

	@Test
	void shouldFailSafelyForInvalidCursor() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();

		assertThatThrownBy(() -> service.aggregate(
				new GovernanceTimelineAggregationRequest(
						new GovernanceTimelineQuery(
								"%%%invalid%%%",
								10,
								GovernanceCursorDirection.NEXT,
								null
						),
						List.of()
				)
		).block())
				.isInstanceOf(GovernanceTimelineCursorDecodeException.class)
				.hasMessage("Invalid timeline cursor.");
	}

	@Test
	void shouldReturnEmptyPageWhenCursorIsAfterOldestEvent() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();
		String cursor = cursorCodec.encode(new GovernanceTimelineCursor(
				Instant.parse("2026-05-14T01:00:00Z"),
				"INCIDENT_TRANSITIONED",
				"INCIDENT_LIFECYCLE:lifecycle-1a"
		));

		GovernanceTimelineAggregationResult result = service.aggregate(
				new GovernanceTimelineAggregationRequest(
						new GovernanceTimelineQuery(
								cursor,
								10,
								GovernanceCursorDirection.NEXT,
								null
						),
						List.of()
				)
		).block();

		assertThat(result.page().items()).isEmpty();
		assertThat(result.page().page().hasNext()).isFalse();
		assertThat(result.page().page().hasPrevious()).isTrue();
		assertThat(result.page().page().previousCursor()).isNull();
		assertThat(result.page().page().nextCursor()).isNull();
	}

	@Test
	void shouldRecordPageSizeMetric() {
		DefaultGovernanceTimelineAggregationService service = serviceWithSampleData();

		GovernanceTimelineAggregationResult result = service.aggregate(
				request(2, null, List.of())
		).block();

		assertThat(result.page().items()).hasSize(2);
		assertThat(meterRegistry.find(GovernanceTimelineMetricName.PAGE_SIZE)
				.tag(GovernanceTimelineMetricTag.MODE, "STRICT")
				.summary()
				.totalAmount()).isEqualTo(2.0);
	}

	private DefaultGovernanceTimelineAggregationService serviceWithSampleData() {
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		InMemoryIncidentLifecycleStore incidentStore =
				new InMemoryIncidentLifecycleStore();
		InMemoryLearningCandidateStore learningStore =
				new InMemoryLearningCandidateStore();

		recommendationStore.save(recommendationOne()).block();
		recommendationStore.save(recommendationTwo()).block();
		approvalStore.save(approvalOne()).block();
		incidentStore.save(lifecycleOne()).block();
		incidentStore.save(lifecycleTwo()).block();
		learningStore.save(learningOne()).block();

		return service(
				recommendationStore,
				approvalStore,
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				incidentStore,
				new InMemoryPostmortemDraftStore(),
				new InMemoryPostmortemReviewStore(),
				learningStore,
				new InMemoryKnowledgePromotionReviewStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgeUpdateApplicationStore()
		);
	}

	private DefaultGovernanceTimelineAggregationService service(
			InMemoryRecommendationRecordStore recommendationStore,
			InMemoryRecommendationApprovalStore approvalStore,
			InMemoryExecutionPlanStore executionPlanStore,
			InMemoryHumanExecutionResultStore humanExecutionStore,
			InMemoryVerificationResultStore verificationStore,
			InMemoryIncidentLifecycleStore incidentStore,
			InMemoryPostmortemDraftStore postmortemDraftStore,
			InMemoryPostmortemReviewStore postmortemReviewStore,
			InMemoryLearningCandidateStore learningStore,
			InMemoryKnowledgePromotionReviewStore promotionReviewStore,
			InMemoryKnowledgePromotionPlanStore promotionPlanStore,
			InMemoryKnowledgeUpdateApplicationStore knowledgeUpdateStore
	) {
		return new DefaultGovernanceTimelineAggregationService(
				recommendationStore,
				approvalStore,
				executionPlanStore,
				humanExecutionStore,
				verificationStore,
				incidentStore,
				postmortemDraftStore,
				postmortemReviewStore,
				learningStore,
				promotionReviewStore,
				promotionPlanStore,
				knowledgeUpdateStore,
				new DefaultGovernanceTimelineProjectionMapper(
						new GovernanceDetailSanitizer()
				),
				cursorCodec,
				metricsRecorder
		);
	}

	private GovernanceTimelineAggregationRequest request(
			Integer limit,
			GovernanceTimelineFilter filter,
			List<GovernanceTimelineAggregationSource> sources
	) {
		return new GovernanceTimelineAggregationRequest(
				new GovernanceTimelineQuery(null, limit, null, filter),
				sources
		);
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

	private com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
		return new com.fasterxml.jackson.databind.ObjectMapper()
				.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
	}
}
