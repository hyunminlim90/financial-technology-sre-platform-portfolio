package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.fintech.sre.agent.incident.lifecycle.InMemoryIncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.learning.application.InMemoryKnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;
import com.fintech.sre.agent.learning.candidate.InMemoryLearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.postmortem.draft.InMemoryPostmortemDraftStore;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.review.InMemoryPostmortemReviewStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class GovernanceIncidentDetailServiceTest {

	@Test
	void shouldBuildIncidentDetailAggregate() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryIncidentLifecycleStore lifecycleStore = new InMemoryIncidentLifecycleStore();
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore = new InMemoryRecommendationApprovalStore();
		InMemoryExecutionPlanStore executionPlanStore = new InMemoryExecutionPlanStore();
		InMemoryHumanExecutionResultStore executionResultStore = new InMemoryHumanExecutionResultStore();
		InMemoryVerificationResultStore verificationStore = new InMemoryVerificationResultStore();
		InMemoryPostmortemDraftStore postmortemDraftStore = new InMemoryPostmortemDraftStore();
		InMemoryPostmortemReviewStore postmortemReviewStore = new InMemoryPostmortemReviewStore();
		InMemoryLearningCandidateStore learningCandidateStore = new InMemoryLearningCandidateStore();
		InMemoryKnowledgeUpdateApplicationStore knowledgeUpdateStore = new InMemoryKnowledgeUpdateApplicationStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		lifecycleStore.save(lifecycle(base.plusSeconds(10))).block();
		recommendationStore.save(recommendation(base.plusSeconds(20))).block();
		approvalStore.save(approval(base.plusSeconds(30))).block();
		executionPlanStore.save(executionPlan(base.plusSeconds(40))).block();
		executionResultStore.save(executionResult(base.plusSeconds(50))).block();
		verificationStore.save(verification(base.plusSeconds(60))).block();
		postmortemDraftStore.save(postmortemDraft(base.plusSeconds(70))).block();
		postmortemReviewStore.save(postmortemReview(base.plusSeconds(80))).block();
		learningCandidateStore.save(learningCandidate(base.plusSeconds(90))).block();
		knowledgeUpdateStore.save(knowledgeUpdate(base.plusSeconds(100))).block();

		GovernanceIncidentDetailService service = service(
				lifecycleStore,
				recommendationStore,
				approvalStore,
				executionPlanStore,
				executionResultStore,
				verificationStore,
				postmortemDraftStore,
				postmortemReviewStore,
				learningCandidateStore,
				knowledgeUpdateStore,
				resilience(),
				registry
		);

		GovernanceIncidentDetailResponse response = service.findByIncidentId("incident-1").block();

		assertThat(response.incidentId()).isEqualTo("incident-1");
		assertThat(response.currentStatus()).isEqualTo("MITIGATING");
		assertThat(response.degradation()).isEqualTo(GovernanceDetailDegradation.none());
		assertThat(response.recommendations()).hasSize(1);
		assertThat(response.approvals()).hasSize(1);
		assertThat(response.executionPlans()).hasSize(1);
		assertThat(response.humanExecutionResults()).hasSize(1);
		assertThat(response.verifications()).hasSize(1);
		assertThat(response.postmortemDrafts()).hasSize(1);
		assertThat(response.postmortemReviews()).hasSize(1);
		assertThat(response.learningCandidates()).hasSize(1);
		assertThat(response.knowledgeUpdates()).hasSize(1);
		assertThat(response.timeline()).isSortedAccordingTo(
				java.util.Comparator.comparing(GovernanceDetailTimelineItem::occurredAt)
		);
		assertThat(response.humanExecutionResults().get(0).summary()).isEqualTo("[redacted]");
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDetailMetricName.DEGRADED_TOTAL)
				.tag("detailType", "incident")
				.counter()).isNull();
	}

	@Test
	void shouldReturnNotFoundWhenIncidentHasNoGovernanceRecords() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceIncidentDetailService service = service(
				new InMemoryIncidentLifecycleStore(),
				new InMemoryRecommendationRecordStore(),
				new InMemoryRecommendationApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new InMemoryPostmortemDraftStore(),
				new InMemoryPostmortemReviewStore(),
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				resilience(),
				registry
		);

		assertThatThrownBy(() -> service.findByIncidentId("missing").block())
				.isInstanceOf(ResponseStatusException.class);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "not_found")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_NOT_FOUND)
				.tag("detailType", "incident")
				.tag("result", "not_found")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnPartialDegradedIncidentDetailWhenChildComponentFails() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryIncidentLifecycleStore lifecycleStore = new InMemoryIncidentLifecycleStore();
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		lifecycleStore.save(lifecycle(base.plusSeconds(10))).block();
		recommendationStore.save(recommendation(base.plusSeconds(20))).block();

		GovernanceIncidentDetailService service = service(
				lifecycleStore,
				recommendationStore,
				failingApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new InMemoryPostmortemDraftStore(),
				new InMemoryPostmortemReviewStore(),
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				resilienceEnabled(),
				registry
		);

		GovernanceIncidentDetailResponse response = service.findByIncidentId("incident-1").block();

		assertThat(response.degradation().degraded()).isTrue();
		assertThat(response.degradation().partialResponse()).isTrue();
		assertThat(response.degradation().failedComponents()).containsExactly("approvals");
		assertThat(response.approvals()).isEmpty();
		assertThat(response.timeline().stream()
				.map(GovernanceDetailTimelineItem::type)
				.toList()).doesNotContain("APPROVAL_DECIDED");
		assertThat(registry.find(GovernanceDetailMetricName.DEGRADED_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "degraded")
				.tag("reason", "component_query_failed")
				.tag("component", "approvals")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldPropagateIncidentChildFailureWhenPartialResponseDisabled() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryIncidentLifecycleStore lifecycleStore = new InMemoryIncidentLifecycleStore();
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		lifecycleStore.save(lifecycle(base.plusSeconds(10))).block();
		recommendationStore.save(recommendation(base.plusSeconds(20))).block();

		GovernanceIncidentDetailService service = service(
				lifecycleStore,
				recommendationStore,
				failingApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new InMemoryPostmortemDraftStore(),
				new InMemoryPostmortemReviewStore(),
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				partialDisabled(),
				new SimpleMeterRegistry()
		);

		assertThatThrownBy(() -> service.findByIncidentId("incident-1").block())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldPropagateIncidentChildFailureWhenFailOpenDisabled() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryIncidentLifecycleStore lifecycleStore = new InMemoryIncidentLifecycleStore();
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		lifecycleStore.save(lifecycle(base.plusSeconds(10))).block();
		recommendationStore.save(recommendation(base.plusSeconds(20))).block();

		GovernanceIncidentDetailService service = service(
				lifecycleStore,
				recommendationStore,
				failingApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new InMemoryPostmortemDraftStore(),
				new InMemoryPostmortemReviewStore(),
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				failOpenDisabled(),
				new SimpleMeterRegistry()
		);

		assertThatThrownBy(() -> service.findByIncidentId("incident-1").block())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldRecordFailureMetricWhenIncidentDetailQueryFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceIncidentDetailService service = service(
				failingIncidentLifecycleStore(),
				new InMemoryRecommendationRecordStore(),
				new InMemoryRecommendationApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new InMemoryPostmortemDraftStore(),
				new InMemoryPostmortemReviewStore(),
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				resilience(),
				registry
		);

		assertThatThrownBy(() -> service.findByIncidentId("incident-1").block())
				.isInstanceOf(IllegalStateException.class);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "failure")
				.counter()
				.count()).isEqualTo(1.0);
	}

	private GovernanceIncidentDetailService service(
			IncidentLifecycleStore lifecycleStore,
			InMemoryRecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			InMemoryExecutionPlanStore executionPlanStore,
			InMemoryHumanExecutionResultStore executionResultStore,
			InMemoryVerificationResultStore verificationStore,
			InMemoryPostmortemDraftStore postmortemDraftStore,
			InMemoryPostmortemReviewStore postmortemReviewStore,
			InMemoryLearningCandidateStore learningCandidateStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateStore,
			GovernanceDetailResilienceProperties properties,
			SimpleMeterRegistry registry
	) {
		return new GovernanceIncidentDetailService(
				lifecycleStore,
				recommendationStore,
				approvalStore,
				executionPlanStore,
				executionResultStore,
				verificationStore,
				postmortemDraftStore,
				postmortemReviewStore,
				learningCandidateStore,
				knowledgeUpdateStore,
				new GovernanceDetailTimelineBuilder(new GovernanceDetailSanitizer()),
				new GovernanceDetailSanitizer(),
				new GovernanceDetailComponentLoader(properties),
				recorder(registry)
		);
	}

	private IncidentLifecycleStore failingIncidentLifecycleStore() {
		return new IncidentLifecycleStore() {
			@Override
			public Mono<IncidentLifecycleRecord> save(IncidentLifecycleRecord record) {
				return Mono.just(record);
			}

			@Override
			public Mono<IncidentLifecycleRecord> findLatestByIncidentId(String incidentId) {
				return Mono.empty();
			}

			@Override
			public Flux<IncidentLifecycleRecord> findByIncidentId(String incidentId) {
				return Flux.error(new IllegalStateException("boom"));
			}

			@Override
			public Flux<IncidentLifecycleRecord> findRecent(int limit) {
				return Flux.empty();
			}
		};
	}

	private RecommendationApprovalStore failingApprovalStore() {
		return new RecommendationApprovalStore() {
			@Override
			public Mono<RecommendationApprovalRecord> save(RecommendationApprovalRecord record) {
				return Mono.just(record);
			}

			@Override
			public Flux<RecommendationApprovalRecord> findByIncidentId(String incidentId) {
				return Flux.error(new IllegalStateException("boom"));
			}

			@Override
			public Flux<RecommendationApprovalRecord> findByRecommendationRecordId(String recommendationRecordId) {
				return Flux.empty();
			}

			@Override
			public Mono<RecommendationApprovalRecord> findLatestByRecommendationRecordId(String recommendationRecordId) {
				return Mono.empty();
			}

			@Override
			public Flux<RecommendationApprovalRecord> findRecent(int limit) {
				return Flux.empty();
			}
		};
	}

	private GovernanceDetailMetricsRecorder recorder(SimpleMeterRegistry registry) {
		return new GovernanceDetailMetricsRecorder(
				new GovernanceMetricsRecorder(registry)
		);
	}

	private GovernanceDetailResilienceProperties resilience() {
		return new GovernanceDetailResilienceProperties();
	}

	private GovernanceDetailResilienceProperties resilienceEnabled() {
		GovernanceDetailResilienceProperties properties = new GovernanceDetailResilienceProperties();
		properties.setEnabled(true);
		return properties;
	}

	private GovernanceDetailResilienceProperties partialDisabled() {
		GovernanceDetailResilienceProperties properties = resilienceEnabled();
		properties.setPartialResponseEnabled(false);
		return properties;
	}

	private GovernanceDetailResilienceProperties failOpenDisabled() {
		GovernanceDetailResilienceProperties properties = resilienceEnabled();
		properties.setFailOpenDetail(false);
		return properties;
	}

	private IncidentLifecycleRecord lifecycle(Instant transitionedAt) {
		return new IncidentLifecycleRecord(
				"lifecycle-1",
				"incident-1",
				IncidentStatus.OPEN,
				IncidentStatus.MITIGATING,
				IncidentTransitionReason.MANUAL_ESCALATION,
				"operator-a",
				"working incident",
				transitionedAt,
				Map.of("secretToken", "hidden")
		);
	}

	private RecommendationRecord recommendation(Instant occurredAt) {
		return new RecommendationRecord(
				"rec-1",
				"incident-1",
				"audit-1",
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				occurredAt,
				1,
				0,
				"ALLOW",
				"PASS",
				List.of("RATE_LIMIT"),
				List.of(),
				Map.of("payload", "hidden")
		);
	}

	private RecommendationApprovalRecord approval(Instant occurredAt) {
		return new RecommendationApprovalRecord(
				"approval-1",
				"rec-1",
				"incident-1",
				RecommendationApprovalStatus.APPROVED,
				"operator-a",
				"approved",
				occurredAt,
				Map.of()
		);
	}

	private RecommendationExecutionPlan executionPlan(Instant occurredAt) {
		return new RecommendationExecutionPlan(
				"plan-1",
				"rec-1",
				"incident-1",
				ExecutionPlanStatus.DRY_RUN_PLAN_CREATED,
				false,
				true,
				"operator-a",
				"plan",
				occurredAt,
				List.of(),
				List.of()
		);
	}

	private HumanExecutionResultRecord executionResult(Instant occurredAt) {
		return new HumanExecutionResultRecord(
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				HumanExecutionStatus.EXECUTED,
				"operator-a",
				"payment payload was copied",
				occurredAt.minusSeconds(20),
				occurredAt.minusSeconds(10),
				occurredAt,
				Map.of("rawLog", "hidden")
		);
	}

	private VerificationResultRecord verification(Instant occurredAt) {
		return new VerificationResultRecord(
				"verification-1",
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				VerificationStatus.VERIFIED,
				"operator-a",
				"verified",
				occurredAt,
				Map.of()
		);
	}

	private PostmortemDraftRecord postmortemDraft(Instant occurredAt) {
		return new PostmortemDraftRecord(
				"draft-1",
				"incident-1",
				PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED,
				"operator-a",
				"draft summary",
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				occurredAt,
				Map.of()
		);
	}

	private PostmortemReviewRecord postmortemReview(Instant occurredAt) {
		return new PostmortemReviewRecord(
				"review-1",
				"draft-1",
				"incident-1",
				PostmortemReviewStatus.APPROVED,
				"reviewer-a",
				"reviewed",
				"review summary",
				occurredAt,
				Map.of()
		);
	}

	private LearningCandidateRecord learningCandidate(Instant occurredAt) {
		return new LearningCandidateRecord(
				"candidate-1",
				"incident-1",
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.REVIEW_REQUIRED,
				"operator-a",
				"candidate summary",
				List.of("runbook update"),
				occurredAt,
				Map.of()
		);
	}

	private KnowledgeUpdateApplicationRecord knowledgeUpdate(Instant occurredAt) {
		return new KnowledgeUpdateApplicationRecord(
				"update-1",
				"incident-1",
				"candidate-1",
				"promotion-plan-1",
				"RUNBOOK",
				KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
				"runbooks/payment.md",
				KnowledgeUpdateChangeType.UPDATED,
				"portfolio",
				"main",
				"abc123",
				"PR-101",
				"operator-a",
				"reviewer-a",
				"approver-a",
				List.of("link-check"),
				occurredAt,
				Map.of()
		);
	}
}
