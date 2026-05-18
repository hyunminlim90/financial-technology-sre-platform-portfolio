package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryRepository;
import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryResult;
import com.fintech.sre.agent.governance.query.GovernanceDashboardTimeBucketResult;
import com.fintech.sre.agent.governance.query.GovernanceQueryMetricName;
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
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.InMemoryHumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.verification.InMemoryVerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class GovernanceDashboardServiceTest {

	@Test
	void shouldBuildSummaryWithLatestIncidentStatusBreakdown() {
		Instant now = Instant.now();
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		InMemoryExecutionPlanStore executionPlanStore =
				new InMemoryExecutionPlanStore();
		InMemoryHumanExecutionResultStore executionResultStore =
				new InMemoryHumanExecutionResultStore();
		InMemoryVerificationResultStore verificationResultStore =
				new InMemoryVerificationResultStore();
		InMemoryIncidentLifecycleStore lifecycleStore =
				new InMemoryIncidentLifecycleStore();
		InMemoryPostmortemDraftStore postmortemDraftStore =
				new InMemoryPostmortemDraftStore();
		InMemoryPostmortemReviewStore postmortemReviewStore =
				new InMemoryPostmortemReviewStore();
		InMemoryLearningCandidateStore learningCandidateStore =
				new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionReviewStore promotionReviewStore =
				new InMemoryKnowledgePromotionReviewStore();
		InMemoryKnowledgePromotionPlanStore promotionPlanStore =
				new InMemoryKnowledgePromotionPlanStore();
		InMemoryKnowledgeUpdateApplicationStore knowledgeUpdateStore =
				new InMemoryKnowledgeUpdateApplicationStore();

		recommendationStore.save(
				recommendation(
						"rec-1",
						"incident-1",
						now.minusSeconds(30)
				)
		).block();
		approvalStore.save(approval(now.minusSeconds(25))).block();
		executionPlanStore.save(executionPlan(now.minusSeconds(20))).block();
		executionResultStore.save(executionResult(now.minusSeconds(15))).block();
		verificationResultStore.save(verification(now.minusSeconds(10))).block();
		lifecycleStore.save(lifecycle("incident-1", IncidentStatus.OPEN, now.minusSeconds(60))).block();
		lifecycleStore.save(lifecycle("incident-1", IncidentStatus.RESOLVED, now.minusSeconds(5))).block();
		postmortemDraftStore.save(postmortemDraft(now.minusSeconds(4))).block();
		postmortemReviewStore.save(postmortemReview(now.minusSeconds(3))).block();
		learningCandidateStore.save(learningCandidate(now.minusSeconds(2))).block();
		promotionReviewStore.save(promotionReview(now.minusSeconds(2))).block();
		promotionPlanStore.save(promotionPlan(now.minusSeconds(1))).block();
		knowledgeUpdateStore.save(knowledgeUpdate(now)).block();

		recommendationStore.save(
				recommendation(
						"rec-2",
						"incident-2",
						now.minusSeconds(8 * 24 * 60 * 60L)
				)
		).block();
		lifecycleStore.save(lifecycle("incident-2", IncidentStatus.RESOLVED, now.minusSeconds(8 * 24 * 60 * 60L))).block();

		GovernanceDashboardService service = new GovernanceDashboardService(
				recommendationStore,
				approvalStore,
				executionPlanStore,
				executionResultStore,
				verificationResultStore,
				lifecycleStore,
				postmortemDraftStore,
				postmortemReviewStore,
				learningCandidateStore,
				promotionReviewStore,
				promotionPlanStore,
				knowledgeUpdateStore,
				recorder(),
				resilience(),
				dashboardRecorder(new SimpleMeterRegistry())
		);

		GovernanceDashboardSummary summary = service.summary(
				new GovernanceDashboardQuery(
						null,
						now.minusSeconds(24 * 60 * 60),
						now.plusSeconds(1)
				)
		).block();

		assertThat(summary.timeRange()).isNotNull();
		assertThat(summary.degradation()).isEqualTo(GovernanceDashboardDegradation.none());
		assertThat(summary.recommendations()).isEqualTo(1);
		assertThat(summary.approvals().byStatus())
				.containsEntry("APPROVED", 1L);
		assertThat(summary.incidents().byStatus())
				.containsEntry("RESOLVED", 1L);
		assertThat(summary.executionPlans().byStatus())
				.containsEntry("DRY_RUN_PLAN_CREATED", 1L);
		assertThat(summary.knowledgeUpdates()).isEqualTo(1);
	}

	@Test
	void shouldUseOptimizedAggregateQueryRepositoryWhenAvailable() {
		Instant now = Instant.now();
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		InMemoryExecutionPlanStore executionPlanStore =
				new InMemoryExecutionPlanStore();
		InMemoryHumanExecutionResultStore executionResultStore =
				new InMemoryHumanExecutionResultStore();
		InMemoryVerificationResultStore verificationResultStore =
				new InMemoryVerificationResultStore();
		InMemoryIncidentLifecycleStore lifecycleStore =
				new InMemoryIncidentLifecycleStore();
		InMemoryPostmortemDraftStore postmortemDraftStore =
				new InMemoryPostmortemDraftStore();
		InMemoryPostmortemReviewStore postmortemReviewStore =
				new InMemoryPostmortemReviewStore();
		InMemoryLearningCandidateStore learningCandidateStore =
				new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionReviewStore promotionReviewStore =
				new InMemoryKnowledgePromotionReviewStore();
		InMemoryKnowledgePromotionPlanStore promotionPlanStore =
				new InMemoryKnowledgePromotionPlanStore();
		InMemoryKnowledgeUpdateApplicationStore knowledgeUpdateStore =
				new InMemoryKnowledgeUpdateApplicationStore();

		recommendationStore.save(recommendation("rec-optimized", "incident-optimized",
				now.minusSeconds(60))).block();

		GovernanceDashboardQueryRepository queryRepository =
				new GovernanceDashboardQueryRepository() {
					@Override
					public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.just(new GovernanceDashboardQueryResult("APPROVED", 7L));
					}

					@Override
					public Flux<GovernanceDashboardQueryResult> findVerificationStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.just(new GovernanceDashboardQueryResult("VERIFIED", 5L));
					}

					@Override
					public Flux<GovernanceDashboardQueryResult> findLatestIncidentStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.just(new GovernanceDashboardQueryResult("RESOLVED", 3L));
					}

					@Override
					public Flux<com.fintech.sre.agent.governance.query.GovernanceDashboardTimeBucketResult> findApprovalStatusBuckets(
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

		GovernanceDashboardService service = new GovernanceDashboardService(
				recommendationStore,
				approvalStore,
				executionPlanStore,
				executionResultStore,
				verificationResultStore,
				lifecycleStore,
				postmortemDraftStore,
				postmortemReviewStore,
				learningCandidateStore,
				promotionReviewStore,
				promotionPlanStore,
				knowledgeUpdateStore,
				queryRepository,
				recorder(),
				resilience(),
				dashboardRecorder(new SimpleMeterRegistry())
		);

		GovernanceDashboardSummary summary = service.summary(
				new GovernanceDashboardQuery(
						null,
						now.minusSeconds(24 * 60 * 60),
						now.plusSeconds(1)
				)
		).block();

		assertThat(summary.approvals().total()).isEqualTo(7);
		assertThat(summary.approvals().byStatus()).containsEntry("APPROVED", 7L);
		assertThat(summary.verifications().total()).isEqualTo(5);
		assertThat(summary.incidents().byStatus()).containsEntry("RESOLVED", 3L);
		assertThat(summary.degradation()).isEqualTo(GovernanceDashboardDegradation.none());
	}

	@Test
	void shouldRecordFallbackMetricsWhenRepositoryIsMissing() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardService service = new GovernanceDashboardService(
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
				new InMemoryKnowledgeUpdateApplicationStore(),
				new GovernanceQueryMetricsRecorder(new GovernanceMetricsRecorder(registry)),
				resilience(),
				dashboardRecorder(registry)
		);

		GovernanceDashboardSummary summary = service.summary(new GovernanceDashboardQuery(
				null,
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(1)
		)).block();

		assertThat(registry.find(GovernanceQueryMetricName.FALLBACK)
				.tag("queryType", "summary")
				.tag("series", "approvalStatus")
				.tag("reason", "repository_missing")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(summary.degradation()).isEqualTo(GovernanceDashboardDegradation.none());
	}

	@Test
	void shouldRecordFailureAndFallbackMetricsWhenOptimizedSummaryQueryFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		approvalStore.save(approval(Instant.now())).block();

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
						return Flux.just(new GovernanceDashboardQueryResult("VERIFIED", 1L));
					}

					@Override
					public Flux<GovernanceDashboardQueryResult> findLatestIncidentStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.just(new GovernanceDashboardQueryResult("OPEN", 1L));
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

		GovernanceDashboardService service = new GovernanceDashboardService(
				new InMemoryRecommendationRecordStore(),
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
				new GovernanceQueryMetricsRecorder(new GovernanceMetricsRecorder(registry)),
				resilience(),
				dashboardRecorder(registry)
		);

		GovernanceDashboardSummary summary = service.summary(new GovernanceDashboardQuery(
				null,
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(1)
		)).block();

		assertThat(registry.find(GovernanceQueryMetricName.FAILURE)
				.tag("queryType", "summary")
				.tag("series", "approvalStatus")
				.tag("reason", "query_failed")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceQueryMetricName.FALLBACK)
				.tag("queryType", "summary")
				.tag("series", "approvalStatus")
				.tag("reason", "query_failed")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(summary.degradation().degraded()).isTrue();
		assertThat(summary.degradation().fallbackUsed()).isTrue();
		assertThat(summary.degradation().reason()).isEqualTo("query_failed");
		assertThat(registry.find(GovernanceDashboardMetricName.DEGRADED)
				.tag("endpoint", "summary")
				.tag("reason", "query_failed")
				.tag("fallbackUsed", "true")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldApplyTimeoutAndReturnDegradedFallbackWhenEnabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		approvalStore.save(approval(Instant.now())).block();

		GovernanceDashboardQueryRepository slowRepository =
				new GovernanceDashboardQueryRepository() {
					@Override
					public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.never();
					}

					@Override
					public Flux<GovernanceDashboardQueryResult> findVerificationStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.just(new GovernanceDashboardQueryResult("VERIFIED", 1L));
					}

					@Override
					public Flux<GovernanceDashboardQueryResult> findLatestIncidentStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.just(new GovernanceDashboardQueryResult("OPEN", 1L));
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

		GovernanceDashboardService service = new GovernanceDashboardService(
				new InMemoryRecommendationRecordStore(),
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
				slowRepository,
				new GovernanceQueryMetricsRecorder(new GovernanceMetricsRecorder(registry)),
				resilienceEnabled(1, true, true),
				dashboardRecorder(registry)
		);

		GovernanceDashboardSummary summary = service.summary(new GovernanceDashboardQuery(
				null,
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(1)
		)).block();

		assertThat(summary.degradation().degraded()).isTrue();
		assertThat(summary.degradation().reason()).isEqualTo("query_timeout");
		assertThat(registry.find(GovernanceQueryMetricName.FAILURE)
				.tag("queryType", "summary")
				.tag("series", "approvalStatus")
				.tag("reason", "query_timeout")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDashboardMetricName.DEGRADED)
				.tag("endpoint", "summary")
				.tag("reason", "query_timeout")
				.tag("fallbackUsed", "true")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldPropagateFailureWhenFallbackIsDisabled() {
		GovernanceDashboardQueryRepository failingRepository = failingSummaryRepository();
		GovernanceDashboardService service = new GovernanceDashboardService(
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
				new InMemoryKnowledgeUpdateApplicationStore(),
				failingRepository,
				recorder(),
				resilienceEnabled(100, false, true),
				dashboardRecorder(new SimpleMeterRegistry())
		);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.summary(
				new GovernanceDashboardQuery(
						null,
						Instant.now().minusSeconds(60),
						Instant.now().plusSeconds(1)
				)
		).block()).isInstanceOf(RuntimeException.class);
	}

	@Test
	void shouldPropagateFailureWhenFailOpenDashboardIsDisabled() {
		GovernanceDashboardQueryRepository failingRepository = failingSummaryRepository();
		GovernanceDashboardService service = new GovernanceDashboardService(
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
				new InMemoryKnowledgeUpdateApplicationStore(),
				failingRepository,
				recorder(),
				resilienceEnabled(100, true, false),
				dashboardRecorder(new SimpleMeterRegistry())
		);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.summary(
				new GovernanceDashboardQuery(
						null,
						Instant.now().minusSeconds(60),
						Instant.now().plusSeconds(1)
				)
		).block()).isInstanceOf(RuntimeException.class);
	}

	private GovernanceQueryMetricsRecorder recorder() {
		return new GovernanceQueryMetricsRecorder(
				new GovernanceMetricsRecorder(new SimpleMeterRegistry())
		);
	}

	private GovernanceDashboardMetricsRecorder dashboardRecorder(
			SimpleMeterRegistry registry
	) {
		return new GovernanceDashboardMetricsRecorder(
				new GovernanceMetricsRecorder(registry)
		);
	}

	private GovernanceQueryResilienceProperties resilience() {
		return resilienceEnabled(false, 1500, true, true);
	}

	private GovernanceQueryResilienceProperties resilienceEnabled(
			int timeoutMs,
			boolean fallbackEnabled,
			boolean failOpenDashboard
	) {
		return resilienceEnabled(true, timeoutMs, fallbackEnabled, failOpenDashboard);
	}

	private GovernanceQueryResilienceProperties resilienceEnabled(
			boolean enabled,
			int timeoutMs,
			boolean fallbackEnabled,
			boolean failOpenDashboard
	) {
		GovernanceQueryResilienceProperties properties =
				new GovernanceQueryResilienceProperties();
		properties.setEnabled(enabled);
		properties.setOptimizedQueryTimeoutMs(timeoutMs);
		properties.setFallbackEnabled(fallbackEnabled);
		properties.setFailOpenDashboard(failOpenDashboard);
		return properties;
	}

	private GovernanceDashboardQueryRepository failingSummaryRepository() {
		return new GovernanceDashboardQueryRepository() {
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
	}

	private RecommendationRecord recommendation(
			String recommendationId,
			String incidentId,
			Instant generatedAt
	) {
		return new RecommendationRecord(
				recommendationId,
				incidentId,
				"audit-1",
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

	private RecommendationApprovalRecord approval(Instant decidedAt) {
		return new RecommendationApprovalRecord(
				"approval-1",
				"rec-1",
				"incident-1",
				RecommendationApprovalStatus.APPROVED,
				"operator-a",
				"approved",
				decidedAt,
				Map.of()
		);
	}

	private RecommendationExecutionPlan executionPlan(Instant createdAt) {
		return new RecommendationExecutionPlan(
				"plan-1",
				"rec-1",
				"incident-1",
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

	private HumanExecutionResultRecord executionResult(Instant recordedAt) {
		return new HumanExecutionResultRecord(
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				HumanExecutionStatus.EXECUTED,
				"operator-a",
				"manual execution",
				Instant.now().minusSeconds(60),
				Instant.now().minusSeconds(30),
				recordedAt,
				Map.of()
		);
	}

	private VerificationResultRecord verification(Instant verifiedAt) {
		return new VerificationResultRecord(
				"verification-1",
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				VerificationStatus.VERIFIED,
				"operator-a",
				"verified",
				verifiedAt,
				Map.of()
		);
	}

	private IncidentLifecycleRecord lifecycle(String incidentId, IncidentStatus status, Instant transitionedAt) {
		return new IncidentLifecycleRecord(
				"lifecycle-" + status.name(),
				incidentId,
				status == IncidentStatus.OPEN ? null : IncidentStatus.MITIGATING,
				status,
				IncidentTransitionReason.INCIDENT_RESOLVED,
				"operator-a",
				"lifecycle",
				transitionedAt,
				Map.of()
		);
	}

	private PostmortemDraftRecord postmortemDraft(Instant createdAt) {
		return new PostmortemDraftRecord(
				"draft-1",
				"incident-1",
				PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED,
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

	private PostmortemReviewRecord postmortemReview(Instant reviewedAt) {
		return new PostmortemReviewRecord(
				"review-1",
				"draft-1",
				"incident-1",
				PostmortemReviewStatus.APPROVED,
				"reviewer-a",
				"reviewed",
				"approved",
				reviewedAt,
				Map.of()
		);
	}

	private LearningCandidateRecord learningCandidate(Instant createdAt) {
		return new LearningCandidateRecord(
				"candidate-1",
				"incident-1",
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.REVIEW_REQUIRED,
				"operator-a",
				"candidate",
				List.of(),
				createdAt,
				Map.of()
		);
	}

	private KnowledgePromotionReviewRecord promotionReview(Instant reviewedAt) {
		return new KnowledgePromotionReviewRecord(
				"promotion-review-1",
				"candidate-1",
				"incident-1",
				KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
				"reviewer-b",
				"reviewed",
				"approved",
				reviewedAt,
				Map.of()
		);
	}

	private KnowledgePromotionPlanRecord promotionPlan(Instant createdAt) {
		return new KnowledgePromotionPlanRecord(
				"promotion-plan-1",
				"candidate-1",
				"incident-1",
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

	private KnowledgeUpdateApplicationRecord knowledgeUpdate(Instant appliedAt) {
		return new KnowledgeUpdateApplicationRecord(
				"knowledge-update-1",
				"incident-1",
				"candidate-1",
				"promotion-plan-1",
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
