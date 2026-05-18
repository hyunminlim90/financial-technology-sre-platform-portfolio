package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryRepository;
import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryResult;
import com.fintech.sre.agent.governance.query.GovernanceDashboardTimeBucketResult;
import com.fintech.sre.agent.governance.query.GovernanceQueryMetricsRecorder;
import com.fintech.sre.agent.governance.query.GovernanceQueryResilienceProperties;
import com.fintech.sre.agent.incident.lifecycle.InMemoryIncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.learning.application.InMemoryKnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;
import com.fintech.sre.agent.learning.candidate.InMemoryLearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.plan.InMemoryKnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanTarget;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionTargetType;
import com.fintech.sre.agent.learning.promotion.InMemoryKnowledgePromotionReviewStore;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;
import com.fintech.sre.agent.postmortem.draft.InMemoryPostmortemDraftStore;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.review.InMemoryPostmortemReviewStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.InMemoryExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.execution.result.InMemoryHumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.verification.InMemoryVerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;

class GovernanceDashboardOverviewServiceTest {

	@Test
	void shouldCombineSummaryBacklogTrendsAndRiskIndicators() {
		Instant now = Instant.now();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceQueryMetricsRecorder queryMetricsRecorder =
				new GovernanceQueryMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);
		GovernanceDashboardMetricsRecorder dashboardMetricsRecorder =
				dashboardRecorder(registry);
		GovernanceQueryResilienceProperties resilienceProperties = resilience();
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		InMemoryExecutionPlanStore executionPlanStore =
				new InMemoryExecutionPlanStore();
		InMemoryHumanExecutionResultStore executionResultStore =
				new InMemoryHumanExecutionResultStore();
		InMemoryVerificationResultStore verificationStore =
				new InMemoryVerificationResultStore();
		InMemoryIncidentLifecycleStore lifecycleStore =
				new InMemoryIncidentLifecycleStore();
		InMemoryPostmortemDraftStore draftStore =
				new InMemoryPostmortemDraftStore();
		InMemoryPostmortemReviewStore reviewStore =
				new InMemoryPostmortemReviewStore();
		InMemoryLearningCandidateStore learningCandidateStore =
				new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionReviewStore promotionReviewStore =
				new InMemoryKnowledgePromotionReviewStore();
		InMemoryKnowledgePromotionPlanStore promotionPlanStore =
				new InMemoryKnowledgePromotionPlanStore();
		InMemoryKnowledgeUpdateApplicationStore knowledgeUpdateStore =
				new InMemoryKnowledgeUpdateApplicationStore();

		recommendationStore.save(recommendation("rec-1", "incident-1", now.minusSeconds(90 * 60))).block();
		approvalStore.save(approval("approval-1", "rec-1", "incident-1",
				RecommendationApprovalStatus.APPROVED, now.minusSeconds(75 * 60))).block();
		executionPlanStore.save(executionPlan("plan-1", "rec-1", "incident-1",
				now.minusSeconds(70 * 60))).block();
		executionResultStore.save(executionResult("result-1", "plan-1", "rec-1", "incident-1",
				now.minusSeconds(60 * 60))).block();
		verificationStore.save(verification("verification-1", "result-1", "plan-1", "rec-1",
				"incident-1", VerificationStatus.REGRESSION_DETECTED, now.minusSeconds(50 * 60))).block();
		lifecycleStore.save(lifecycle("incident-1", IncidentStatus.MITIGATING,
				now.minusSeconds(45 * 60))).block();
		draftStore.save(postmortemDraft("draft-1", "incident-1",
				PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED, now.minusSeconds(40 * 60))).block();
		reviewStore.save(postmortemReview("review-1", "draft-1", "incident-1",
				PostmortemReviewStatus.NEEDS_REVISION, now.minusSeconds(35 * 60))).block();
		learningCandidateStore.save(learningCandidate("candidate-1", "incident-1",
				LearningCandidateStatus.REVIEW_REQUIRED, now.minusSeconds(30 * 60))).block();
		promotionReviewStore.save(promotionReview("promotion-review-1", "candidate-1", "incident-1",
				now.minusSeconds(25 * 60))).block();
		promotionPlanStore.save(promotionPlan("promotion-plan-1", "candidate-1", "incident-1",
				now.minusSeconds(20 * 60))).block();
		knowledgeUpdateStore.save(knowledgeUpdate("update-1", "candidate-1", "promotion-plan-1",
				"incident-1", now.minusSeconds(10 * 60))).block();

		GovernanceDashboardOverviewService service =
				new GovernanceDashboardOverviewService(
						new GovernanceDashboardService(
								recommendationStore,
								approvalStore,
								executionPlanStore,
								executionResultStore,
								verificationStore,
								lifecycleStore,
								draftStore,
								reviewStore,
								learningCandidateStore,
								promotionReviewStore,
								promotionPlanStore,
								knowledgeUpdateStore,
								queryMetricsRecorder,
								resilienceProperties,
								dashboardMetricsRecorder
						),
						new GovernanceDashboardBacklogService(
								approvalStore,
								executionPlanStore,
								executionResultStore,
								verificationStore,
								lifecycleStore,
								draftStore,
								learningCandidateStore,
								promotionPlanStore,
								knowledgeUpdateStore
						),
						new GovernanceDashboardTrendService(
								recommendationStore,
								approvalStore,
								verificationStore,
								lifecycleStore,
								reviewStore,
								learningCandidateStore,
								knowledgeUpdateStore,
								queryMetricsRecorder,
								resilienceProperties,
								dashboardMetricsRecorder
						),
						new GovernanceDashboardRiskService(
								approvalStore,
								verificationStore,
								lifecycleStore,
								learningCandidateStore,
								promotionPlanStore,
								reviewStore
						),
						dashboardMetricsRecorder
				);

		GovernanceDashboardOverview overview = service.overview(
				new GovernanceDashboardTrendQuery(
						null,
						now.minusSeconds(24 * 60 * 60),
						now.plusSeconds(1),
						"1h"
				)
		).block();

		assertThat(overview.summary()).isNotNull();
		assertThat(overview.backlog()).isNotNull();
		assertThat(overview.trends()).isNotNull();
		assertThat(overview.riskIndicators()).isNotNull();
		assertThat(overview.degradation()).isEqualTo(GovernanceDashboardDegradation.none());
		assertThat(overview.timeRange()).isEqualTo(overview.summary().timeRange());
		assertThat(overview.backlog().timeRange()).isEqualTo(overview.summary().timeRange());
		assertThat(overview.riskIndicators().timeRange()).isEqualTo(overview.summary().timeRange());
		assertThat(overview.trends().bucketSize()).isEqualTo("1h");
	}

	@Test
	void shouldIncludeSummaryOrTrendDegradationInOverview() {
		Instant now = Instant.now();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceQueryMetricsRecorder queryMetricsRecorder =
				new GovernanceQueryMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);
		GovernanceDashboardMetricsRecorder dashboardMetricsRecorder =
				dashboardRecorder(registry);
		GovernanceQueryResilienceProperties resilienceProperties = new GovernanceQueryResilienceProperties();
		resilienceProperties.setEnabled(true);
		resilienceProperties.setOptimizedQueryTimeoutMs(100);
		resilienceProperties.setFallbackEnabled(true);
		resilienceProperties.setFailOpenDashboard(true);

		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		approvalStore.save(approval("approval-1", "rec-1", "incident-1",
				RecommendationApprovalStatus.APPROVED, now.minusSeconds(60))).block();

		GovernanceDashboardQueryRepository failingRepository =
				new GovernanceDashboardQueryRepository() {
					@Override
					public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.error(new IllegalStateException("boom"));
					}

					@Override
					public Flux<GovernanceDashboardQueryResult> findVerificationStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.empty();
					}

					@Override
					public Flux<GovernanceDashboardQueryResult> findLatestIncidentStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.empty();
					}

					@Override
					public Flux<GovernanceDashboardTimeBucketResult> findApprovalStatusBuckets(
							GovernanceDashboardTimeRange range,
							GovernanceDashboardBucketSize bucketSize
					) {
						return Flux.empty();
					}

					@Override
					public Flux<GovernanceDashboardTimeBucketResult> findVerificationStatusBuckets(
							GovernanceDashboardTimeRange range,
							GovernanceDashboardBucketSize bucketSize
					) {
						return Flux.empty();
					}

					@Override
					public Flux<GovernanceDashboardTimeBucketResult> findIncidentLifecycleStatusBuckets(
							GovernanceDashboardTimeRange range,
							GovernanceDashboardBucketSize bucketSize
					) {
						return Flux.empty();
					}
				};

		GovernanceDashboardOverviewService service =
				new GovernanceDashboardOverviewService(
						new GovernanceDashboardService(
								recommendationStore,
								approvalStore,
								new InMemoryExecutionPlanStore(),
								new InMemoryHumanExecutionResultStore(),
								new InMemoryVerificationResultStore(),
								new InMemoryIncidentLifecycleStore(),
								new InMemoryPostmortemDraftStore(),
								new InMemoryPostmortemReviewStore(),
								new InMemoryLearningCandidateStore(),
								new InMemoryKnowledgePromotionReviewStore(),
								new InMemoryKnowledgePromotionPlanStore(),
								new InMemoryKnowledgeUpdateApplicationStore(),
								failingRepository,
								queryMetricsRecorder,
								resilienceProperties,
								dashboardMetricsRecorder
						),
						new GovernanceDashboardBacklogService(
								approvalStore,
								new InMemoryExecutionPlanStore(),
								new InMemoryHumanExecutionResultStore(),
								new InMemoryVerificationResultStore(),
								new InMemoryIncidentLifecycleStore(),
								new InMemoryPostmortemDraftStore(),
								new InMemoryLearningCandidateStore(),
								new InMemoryKnowledgePromotionPlanStore(),
								new InMemoryKnowledgeUpdateApplicationStore()
						),
						new GovernanceDashboardTrendService(
								recommendationStore,
								approvalStore,
								new InMemoryVerificationResultStore(),
								new InMemoryIncidentLifecycleStore(),
								new InMemoryPostmortemReviewStore(),
								new InMemoryLearningCandidateStore(),
								new InMemoryKnowledgeUpdateApplicationStore(),
								queryMetricsRecorder,
								resilience(),
								dashboardMetricsRecorder
						),
						new GovernanceDashboardRiskService(
								approvalStore,
								new InMemoryVerificationResultStore(),
								new InMemoryIncidentLifecycleStore(),
								new InMemoryLearningCandidateStore(),
								new InMemoryKnowledgePromotionPlanStore(),
								new InMemoryPostmortemReviewStore()
						),
						dashboardMetricsRecorder
				);

		GovernanceDashboardOverview overview = service.overview(
				new GovernanceDashboardTrendQuery(
						null,
						now.minusSeconds(24 * 60 * 60),
						now.plusSeconds(1),
						"1h"
				)
		).block();

		assertThat(overview.degradation().degraded()).isTrue();
		assertThat(overview.summary().degradation().degraded()).isTrue();
		assertThat(registry.find(GovernanceDashboardMetricName.DEGRADED)
				.tag("endpoint", "overview")
				.tag("reason", "query_failed")
				.tag("fallbackUsed", "true")
				.counter()
				.count()).isEqualTo(1.0);
	}

	private GovernanceDashboardMetricsRecorder dashboardRecorder(
			SimpleMeterRegistry registry
	) {
		return new GovernanceDashboardMetricsRecorder(
				new GovernanceMetricsRecorder(registry)
		);
	}

	private GovernanceQueryResilienceProperties resilience() {
		GovernanceQueryResilienceProperties properties =
				new GovernanceQueryResilienceProperties();
		properties.setEnabled(false);
		properties.setOptimizedQueryTimeoutMs(1500);
		properties.setFallbackEnabled(true);
		properties.setFailOpenDashboard(true);
		return properties;
	}

	private RecommendationRecord recommendation(
			String recommendationId,
			String incidentId,
			Instant generatedAt
	) {
		return new RecommendationRecord(
				recommendationId,
				incidentId,
				"audit-" + recommendationId,
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				generatedAt,
				1,
				0,
				"ALLOW",
				"PASS",
				List.of("RATE_LIMIT"),
				List.of(),
				Map.of()
		);
	}

	private RecommendationApprovalRecord approval(
			String approvalId,
			String recommendationId,
			String incidentId,
			RecommendationApprovalStatus status,
			Instant decidedAt
	) {
		return new RecommendationApprovalRecord(
				approvalId,
				recommendationId,
				incidentId,
				status,
				"operator-a",
				"reason",
				decidedAt,
				Map.of()
		);
	}

	private RecommendationExecutionPlan executionPlan(
			String planId,
			String recommendationId,
			String incidentId,
			Instant createdAt
	) {
		return new RecommendationExecutionPlan(
				planId,
				recommendationId,
				incidentId,
				ExecutionPlanStatus.DRY_RUN_PLAN_CREATED,
				false,
				true,
				"operator-a",
				"plan",
				createdAt,
				List.of(),
				List.of()
		);
	}

	private HumanExecutionResultRecord executionResult(
			String resultId,
			String planId,
			String recommendationId,
			String incidentId,
			Instant recordedAt
	) {
		return new HumanExecutionResultRecord(
				resultId,
				planId,
				recommendationId,
				incidentId,
				HumanExecutionStatus.EXECUTED,
				"operator-a",
				"executed",
				recordedAt.minusSeconds(30),
				recordedAt.minusSeconds(10),
				recordedAt,
				Map.of()
		);
	}

	private VerificationResultRecord verification(
			String verificationId,
			String executionResultId,
			String executionPlanId,
			String recommendationId,
			String incidentId,
			VerificationStatus status,
			Instant verifiedAt
	) {
		return new VerificationResultRecord(
				verificationId,
				executionResultId,
				executionPlanId,
				recommendationId,
				incidentId,
				status,
				"operator-a",
				"verified",
				verifiedAt,
				Map.of()
		);
	}

	private IncidentLifecycleRecord lifecycle(
			String incidentId,
			IncidentStatus status,
			Instant transitionedAt
	) {
		return new IncidentLifecycleRecord(
				"lifecycle-" + incidentId + "-" + status.name(),
				incidentId,
				IncidentStatus.OPEN,
				status,
				IncidentTransitionReason.MITIGATION_IN_PROGRESS,
				"operator-a",
				"lifecycle",
				transitionedAt,
				Map.of()
		);
	}

	private PostmortemDraftRecord postmortemDraft(
			String draftId,
			String incidentId,
			PostmortemDraftStatus status,
			Instant createdAt
	) {
		return new PostmortemDraftRecord(
				draftId,
				incidentId,
				status,
				"operator-a",
				"summary",
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				createdAt,
				Map.of()
		);
	}

	private PostmortemReviewRecord postmortemReview(
			String reviewId,
			String draftId,
			String incidentId,
			PostmortemReviewStatus status,
			Instant reviewedAt
	) {
		return new PostmortemReviewRecord(
				reviewId,
				draftId,
				incidentId,
				status,
				"reviewer-a",
				"reason",
				"summary",
				reviewedAt,
				Map.of()
		);
	}

	private LearningCandidateRecord learningCandidate(
			String candidateId,
			String incidentId,
			LearningCandidateStatus status,
			Instant createdAt
	) {
		return new LearningCandidateRecord(
				candidateId,
				incidentId,
				"draft-" + candidateId,
				"review-" + candidateId,
				LearningCandidateType.RUNBOOK_UPDATE,
				status,
				"operator-a",
				"summary",
				List.of(),
				createdAt,
				Map.of()
		);
	}

	private KnowledgePromotionReviewRecord promotionReview(
			String reviewId,
			String candidateId,
			String incidentId,
			Instant reviewedAt
	) {
		return new KnowledgePromotionReviewRecord(
				reviewId,
				candidateId,
				incidentId,
				KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
				"reviewer-a",
				"reason",
				"summary",
				reviewedAt,
				Map.of()
		);
	}

	private KnowledgePromotionPlanRecord promotionPlan(
			String planId,
			String candidateId,
			String incidentId,
			Instant createdAt
	) {
		return new KnowledgePromotionPlanRecord(
				planId,
				candidateId,
				incidentId,
				KnowledgePromotionPlanStatus.PLAN_CREATED,
				"planner-a",
				"plan",
				List.of(new KnowledgePromotionPlanTarget(
						KnowledgePromotionTargetType.RUNBOOK,
						"runbooks/payment/payment-api-runbook.md",
						"summary",
						List.of(),
						List.of()
				)),
				List.of(),
				List.of(),
				createdAt,
				Map.of()
		);
	}

	private KnowledgeUpdateApplicationRecord knowledgeUpdate(
			String updateId,
			String candidateId,
			String promotionPlanId,
			String incidentId,
			Instant appliedAt
	) {
		return new KnowledgeUpdateApplicationRecord(
				updateId,
				incidentId,
				candidateId,
				promotionPlanId,
				"RUNBOOK",
				KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
				"runbooks/payment/payment-api-runbook.md",
				KnowledgeUpdateChangeType.UPDATED,
				"portfolio",
				"main",
				"a1b2c3d4",
				"PR-101",
				"operator-a",
				"reviewer-b",
				"approver-c",
				List.of(),
				appliedAt,
				Map.of()
		);
	}
}
