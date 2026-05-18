package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.fintech.sre.agent.postmortem.review.InMemoryPostmortemReviewStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.verification.InMemoryVerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;

class GovernanceDashboardTrendServiceTest {

	@Test
	void shouldBuildTrendSummaryByBucket() {
		Instant now = Instant.now();
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		InMemoryVerificationResultStore verificationStore =
				new InMemoryVerificationResultStore();
		InMemoryIncidentLifecycleStore lifecycleStore =
				new InMemoryIncidentLifecycleStore();
		InMemoryPostmortemReviewStore postmortemReviewStore =
				new InMemoryPostmortemReviewStore();
		InMemoryLearningCandidateStore learningCandidateStore =
				new InMemoryLearningCandidateStore();
		InMemoryKnowledgeUpdateApplicationStore knowledgeUpdateStore =
				new InMemoryKnowledgeUpdateApplicationStore();

		recommendationStore.save(recommendation("rec-1", now.minusSeconds(90 * 60), "ALLOW")).block();
		recommendationStore.save(recommendation("rec-2", now.minusSeconds(30 * 60), "ALLOW")).block();
		recommendationStore.save(recommendation("rec-3", now.minusSeconds(8 * 24 * 60 * 60L), "BLOCK")).block();

		approvalStore.save(approval("approval-1", "rec-1", RecommendationApprovalStatus.APPROVED,
				now.minusSeconds(75 * 60))).block();
		approvalStore.save(approval("approval-2", "rec-2", RecommendationApprovalStatus.REJECTED,
				now.minusSeconds(20 * 60))).block();

		verificationStore.save(verification("verification-1", VerificationStatus.VERIFIED,
				now.minusSeconds(25 * 60))).block();

		lifecycleStore.save(lifecycle("incident-1", IncidentStatus.MITIGATING,
				now.minusSeconds(70 * 60))).block();
		lifecycleStore.save(lifecycle("incident-1", IncidentStatus.RESOLVED,
				now.minusSeconds(10 * 60))).block();

		postmortemReviewStore.save(postmortemReview("review-1", PostmortemReviewStatus.APPROVED,
				now.minusSeconds(15 * 60))).block();
		learningCandidateStore.save(learningCandidate("candidate-1", LearningCandidateStatus.REVIEW_REQUIRED,
				now.minusSeconds(12 * 60))).block();
		knowledgeUpdateStore.save(knowledgeUpdate("update-1", KnowledgeUpdateChangeType.UPDATED,
				now.minusSeconds(5 * 60))).block();

		GovernanceDashboardTrendService service =
				new GovernanceDashboardTrendService(
						recommendationStore,
						approvalStore,
						verificationStore,
						lifecycleStore,
						postmortemReviewStore,
						learningCandidateStore,
						knowledgeUpdateStore,
						recorder(),
						resilience(),
						dashboardRecorder(new SimpleMeterRegistry())
				);

		GovernanceDashboardTrendSummary summary = service.trends(
				new GovernanceDashboardTrendQuery(
						null,
						now.minusSeconds(2 * 60 * 60),
						now.plusSeconds(1),
						"1h"
				)
		).block();

		assertThat(summary.series()).isNotEmpty();
		assertThat(summary.degradation()).isEqualTo(GovernanceDashboardDegradation.none());
		assertThat(summary.bucketSize()).isEqualTo("1h");

		GovernanceTrendSeries approvals = findSeries(summary, "approvalDecisions");
		assertThat(approvals.points())
				.anySatisfy(point -> assertThat(point.byStatus())
						.containsEntry("APPROVED", 1L));

		GovernanceTrendSeries recommendations =
				findSeries(summary, "recommendationsCreated");
		assertThat(recommendations.points().stream().mapToLong(GovernanceTrendPoint::total).sum())
				.isEqualTo(2);

		GovernanceTrendSeries updates = findSeries(summary, "knowledgeUpdates");
		assertThat(updates.points())
				.anySatisfy(point -> assertThat(point.byStatus())
						.containsEntry("UPDATED", 1L));
	}

	@Test
	void shouldRejectTooManyBuckets() {
		GovernanceDashboardTrendService service =
				new GovernanceDashboardTrendService(
						new InMemoryRecommendationRecordStore(),
						new InMemoryRecommendationApprovalStore(),
						new InMemoryVerificationResultStore(),
						new InMemoryIncidentLifecycleStore(),
						new InMemoryPostmortemReviewStore(),
						new InMemoryLearningCandidateStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						recorder(),
						resilience(),
						dashboardRecorder(new SimpleMeterRegistry())
				);

		assertThatThrownBy(() -> service.trends(
				new GovernanceDashboardTrendQuery(
						null,
						Instant.parse("2026-01-01T00:00:00Z"),
						Instant.parse("2026-01-07T00:00:00Z"),
						"15m"
				)
		).block())
				.isInstanceOf(GovernanceDashboardRejectedException.class)
				.hasMessageContaining("<= 500");
	}

	@Test
	void shouldUseOptimizedTrendQueryRepositoryAndKeepEmptyBuckets() {
		Instant from = Instant.parse("2026-05-14T00:00:00Z");
		Instant to = Instant.parse("2026-05-14T03:00:00Z");

		GovernanceDashboardQueryRepository queryRepository =
				new GovernanceDashboardQueryRepository() {
					@Override
					public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.empty();
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
						return Flux.just(
								new GovernanceDashboardTimeBucketResult(from, "APPROVED", 2L),
								new GovernanceDashboardTimeBucketResult(
										from.plusSeconds(2 * 60 * 60),
										"REJECTED",
										1L
								)
						);
					}

					@Override
					public Flux<GovernanceDashboardTimeBucketResult> findVerificationStatusBuckets(
							GovernanceDashboardTimeRange range,
							GovernanceDashboardBucketSize bucketSize
					) {
						return Flux.just(
								new GovernanceDashboardTimeBucketResult(
										from.plusSeconds(60 * 60),
										"VERIFIED",
										1L
								)
						);
					}

					@Override
					public Flux<GovernanceDashboardTimeBucketResult> findIncidentLifecycleStatusBuckets(
							GovernanceDashboardTimeRange range,
							GovernanceDashboardBucketSize bucketSize
					) {
						return Flux.just(
								new GovernanceDashboardTimeBucketResult(
										from.plusSeconds(60 * 60),
										"RESOLVED",
										1L
								)
						);
					}
				};

		GovernanceDashboardTrendService service =
				new GovernanceDashboardTrendService(
						new InMemoryRecommendationRecordStore(),
						new InMemoryRecommendationApprovalStore(),
						new InMemoryVerificationResultStore(),
						new InMemoryIncidentLifecycleStore(),
						new InMemoryPostmortemReviewStore(),
						new InMemoryLearningCandidateStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						queryRepository,
						recorder(),
						resilience(),
						dashboardRecorder(new SimpleMeterRegistry())
				);

		GovernanceDashboardTrendSummary summary = service.trends(
				new GovernanceDashboardTrendQuery(
						null,
						from,
						to,
						"1h"
				)
		).block();

		GovernanceTrendSeries approvals = findSeries(summary, "approvalDecisions");
		assertThat(approvals.points()).hasSize(3);
		assertThat(approvals.points().get(0).byStatus()).containsEntry("APPROVED", 2L);
		assertThat(approvals.points().get(1).total()).isZero();
		assertThat(approvals.points().get(2).byStatus()).containsEntry("REJECTED", 1L);

		GovernanceTrendSeries verifications = findSeries(summary, "verificationResults");
		assertThat(verifications.points().get(0).total()).isZero();
		assertThat(verifications.points().get(1).byStatus()).containsEntry("VERIFIED", 1L);

		GovernanceTrendSeries incidents = findSeries(summary, "incidentLifecycleTransitions");
		assertThat(incidents.points().get(1).byStatus()).containsEntry("RESOLVED", 1L);
		assertThat(summary.degradation()).isEqualTo(GovernanceDashboardDegradation.none());
	}

	@Test
	void shouldRecordFallbackMetricsWhenTrendRepositoryIsMissing() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardTrendService service =
				new GovernanceDashboardTrendService(
						new InMemoryRecommendationRecordStore(),
						new InMemoryRecommendationApprovalStore(),
						new InMemoryVerificationResultStore(),
						new InMemoryIncidentLifecycleStore(),
						new InMemoryPostmortemReviewStore(),
						new InMemoryLearningCandidateStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						new GovernanceQueryMetricsRecorder(new GovernanceMetricsRecorder(registry)),
						resilience(),
						dashboardRecorder(registry)
				);

		GovernanceDashboardTrendSummary summary = service.trends(new GovernanceDashboardTrendQuery(
				null,
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(1),
				"1h"
		)).block();

		assertThat(registry.find(GovernanceQueryMetricName.FALLBACK)
				.tag("queryType", "trend")
				.tag("series", "approvalDecisions")
				.tag("reason", "repository_missing")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(summary.degradation()).isEqualTo(GovernanceDashboardDegradation.none());
	}

	@Test
	void shouldRecordFailureAndFallbackMetricsWhenOptimizedTrendQueryFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardQueryRepository failingRepository =
				new GovernanceDashboardQueryRepository() {
					@Override
					public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.empty();
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
						return Flux.error(new IllegalStateException("boom"));
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

		GovernanceDashboardTrendService service =
				new GovernanceDashboardTrendService(
						new InMemoryRecommendationRecordStore(),
						new InMemoryRecommendationApprovalStore(),
						new InMemoryVerificationResultStore(),
						new InMemoryIncidentLifecycleStore(),
						new InMemoryPostmortemReviewStore(),
						new InMemoryLearningCandidateStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						failingRepository,
						new GovernanceQueryMetricsRecorder(new GovernanceMetricsRecorder(registry)),
						resilience(),
						dashboardRecorder(registry)
				);

		GovernanceDashboardTrendSummary summary = service.trends(new GovernanceDashboardTrendQuery(
				null,
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(1),
				"1h"
		)).block();

		assertThat(registry.find(GovernanceQueryMetricName.FAILURE)
				.tag("queryType", "trend")
				.tag("series", "approvalDecisions")
				.tag("reason", "query_failed")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceQueryMetricName.FALLBACK)
				.tag("queryType", "trend")
				.tag("series", "approvalDecisions")
				.tag("reason", "query_failed")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(summary.degradation().degraded()).isTrue();
		assertThat(summary.degradation().reason()).isEqualTo("query_failed");
		assertThat(registry.find(GovernanceDashboardMetricName.DEGRADED)
				.tag("endpoint", "trends")
				.tag("reason", "query_failed")
				.tag("fallbackUsed", "true")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldApplyTimeoutAndReturnDegradedTrendFallbackWhenEnabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardQueryRepository slowRepository =
				new GovernanceDashboardQueryRepository() {
					@Override
					public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
							GovernanceDashboardTimeRange range
					) {
						return Flux.empty();
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
						return Flux.never();
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

		GovernanceDashboardTrendService service =
				new GovernanceDashboardTrendService(
						new InMemoryRecommendationRecordStore(),
						new InMemoryRecommendationApprovalStore(),
						new InMemoryVerificationResultStore(),
						new InMemoryIncidentLifecycleStore(),
						new InMemoryPostmortemReviewStore(),
						new InMemoryLearningCandidateStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						slowRepository,
						new GovernanceQueryMetricsRecorder(new GovernanceMetricsRecorder(registry)),
						resilienceEnabled(1, true, true),
						dashboardRecorder(registry)
				);

		GovernanceDashboardTrendSummary summary = service.trends(new GovernanceDashboardTrendQuery(
				null,
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(1),
				"1h"
		)).block();

		assertThat(summary.degradation().degraded()).isTrue();
		assertThat(summary.degradation().reason()).isEqualTo("query_timeout");
		assertThat(registry.find(GovernanceQueryMetricName.FAILURE)
				.tag("queryType", "trend")
				.tag("series", "approvalDecisions")
				.tag("reason", "query_timeout")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDashboardMetricName.DEGRADED)
				.tag("endpoint", "trends")
				.tag("reason", "query_timeout")
				.tag("fallbackUsed", "true")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldPropagateTrendFailureWhenFallbackIsDisabled() {
		GovernanceDashboardTrendService service =
				new GovernanceDashboardTrendService(
						new InMemoryRecommendationRecordStore(),
						new InMemoryRecommendationApprovalStore(),
						new InMemoryVerificationResultStore(),
						new InMemoryIncidentLifecycleStore(),
						new InMemoryPostmortemReviewStore(),
						new InMemoryLearningCandidateStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						failingTrendRepository(),
						recorder(),
						resilienceEnabled(100, false, true),
						dashboardRecorder(new SimpleMeterRegistry())
				);

		assertThatThrownBy(() -> service.trends(new GovernanceDashboardTrendQuery(
				null,
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(1),
				"1h"
		)).block()).isInstanceOf(RuntimeException.class);
	}

	@Test
	void shouldPropagateTrendFailureWhenFailOpenDashboardIsDisabled() {
		GovernanceDashboardTrendService service =
				new GovernanceDashboardTrendService(
						new InMemoryRecommendationRecordStore(),
						new InMemoryRecommendationApprovalStore(),
						new InMemoryVerificationResultStore(),
						new InMemoryIncidentLifecycleStore(),
						new InMemoryPostmortemReviewStore(),
						new InMemoryLearningCandidateStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						failingTrendRepository(),
						recorder(),
						resilienceEnabled(100, true, false),
						dashboardRecorder(new SimpleMeterRegistry())
				);

		assertThatThrownBy(() -> service.trends(new GovernanceDashboardTrendQuery(
				null,
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(1),
				"1h"
		)).block()).isInstanceOf(RuntimeException.class);
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

	private GovernanceDashboardQueryRepository failingTrendRepository() {
		return new GovernanceDashboardQueryRepository() {
			@Override
			public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
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
				return Flux.error(new IllegalStateException("boom"));
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

	private GovernanceTrendSeries findSeries(
			GovernanceDashboardTrendSummary summary,
			String name
	) {
		return summary.series().stream()
				.filter(series -> name.equals(series.name()))
				.findFirst()
				.orElseThrow();
	}

	private RecommendationRecord recommendation(
			String recommendationId,
			Instant generatedAt,
			String policyDecision
	) {
		return new RecommendationRecord(
				recommendationId,
				"incident-" + recommendationId,
				"audit-" + recommendationId,
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				generatedAt,
				1,
				0,
				policyDecision,
				"PASS",
				List.of("RATE_LIMIT"),
				List.of(),
				Map.of()
		);
	}

	private RecommendationApprovalRecord approval(
			String approvalId,
			String recommendationId,
			RecommendationApprovalStatus status,
			Instant decidedAt
	) {
		return new RecommendationApprovalRecord(
				approvalId,
				recommendationId,
				"incident-" + recommendationId,
				status,
				"operator-a",
				"reason",
				decidedAt,
				Map.of()
		);
	}

	private VerificationResultRecord verification(
			String verificationId,
			VerificationStatus status,
			Instant verifiedAt
	) {
		return new VerificationResultRecord(
				verificationId,
				"execution-" + verificationId,
				"plan-" + verificationId,
				"rec-" + verificationId,
				"incident-" + verificationId,
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
				status == IncidentStatus.MITIGATING ? IncidentStatus.OPEN : IncidentStatus.MITIGATING,
				status,
				IncidentTransitionReason.INCIDENT_RESOLVED,
				"operator-a",
				"lifecycle",
				transitionedAt,
				Map.of()
		);
	}

	private PostmortemReviewRecord postmortemReview(
			String reviewId,
			PostmortemReviewStatus status,
			Instant reviewedAt
	) {
		return new PostmortemReviewRecord(
				reviewId,
				"draft-" + reviewId,
				"incident-" + reviewId,
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
			LearningCandidateStatus status,
			Instant createdAt
	) {
		return new LearningCandidateRecord(
				candidateId,
				"incident-" + candidateId,
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

	private KnowledgeUpdateApplicationRecord knowledgeUpdate(
			String updateId,
			KnowledgeUpdateChangeType changeType,
			Instant appliedAt
	) {
		return new KnowledgeUpdateApplicationRecord(
				updateId,
				"incident-" + updateId,
				"candidate-" + updateId,
				"plan-" + updateId,
				"RUNBOOK",
				KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
				"runbooks/payment/payment-api-runbook.md",
				changeType,
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
