package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
import com.fintech.sre.agent.postmortem.draft.InMemoryPostmortemDraftStore;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.execution.InMemoryExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.execution.result.InMemoryHumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.verification.InMemoryVerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

class GovernanceDashboardBacklogServiceTest {

	@Test
	void shouldBuildBacklogSummaryWithinTimeRange() {
		Instant now = Instant.now();
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
		InMemoryPostmortemDraftStore draftStore =
				new InMemoryPostmortemDraftStore();
		InMemoryLearningCandidateStore learningCandidateStore =
				new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionPlanStore promotionPlanStore =
				new InMemoryKnowledgePromotionPlanStore();
		InMemoryKnowledgeUpdateApplicationStore knowledgeUpdateStore =
				new InMemoryKnowledgeUpdateApplicationStore();

		approvalStore.save(approval("approval-pending", "rec-pending", "incident-1",
				RecommendationApprovalStatus.PENDING, now.minusSeconds(50))).block();
		approvalStore.save(approval("approval-approved", "rec-approved", "incident-2",
				RecommendationApprovalStatus.APPROVED, now.minusSeconds(45))).block();
		approvalStore.save(approval("approval-with-plan", "rec-with-plan", "incident-3",
				RecommendationApprovalStatus.APPROVED, now.minusSeconds(40))).block();
		executionPlanStore.save(executionPlan("plan-1", "rec-with-plan", "incident-3",
				now.minusSeconds(35))).block();

		executionResultStore.save(executionResult("result-unverified", "plan-2", "rec-4",
				"incident-4", now.minusSeconds(30))).block();
		executionResultStore.save(executionResult("result-verified", "plan-3", "rec-5",
				"incident-5", now.minusSeconds(25))).block();
		verificationResultStore.save(verification("verification-1", "result-verified",
				"plan-3", "rec-5", "incident-5", now.minusSeconds(20))).block();

		lifecycleStore.save(lifecycle("incident-open", IncidentStatus.OPEN, now.minusSeconds(60))).block();
		lifecycleStore.save(lifecycle("incident-open", IncidentStatus.MITIGATING, now.minusSeconds(10))).block();
		lifecycleStore.save(lifecycle("incident-resolved", IncidentStatus.RESOLVED, now.minusSeconds(15))).block();

		draftStore.save(postmortemDraft("draft-review", "incident-6",
				PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED, now.minusSeconds(12))).block();
		draftStore.save(postmortemDraft("draft-approved", "incident-7",
				PostmortemDraftStatus.APPROVED, now.minusSeconds(11))).block();

		learningCandidateStore.save(learningCandidate("candidate-review", "incident-8",
				LearningCandidateStatus.REVIEW_REQUIRED, now.minusSeconds(9))).block();
		learningCandidateStore.save(learningCandidate("candidate-approved", "incident-9",
				LearningCandidateStatus.APPROVED, now.minusSeconds(8))).block();

		promotionPlanStore.save(promotionPlan("promotion-plan-open", "candidate-review",
				"incident-8", now.minusSeconds(7))).block();
		promotionPlanStore.save(promotionPlan("promotion-plan-applied", "candidate-approved",
				"incident-9", now.minusSeconds(6))).block();
		knowledgeUpdateStore.save(knowledgeUpdate("knowledge-update-1", "candidate-approved",
				"promotion-plan-applied", "incident-9", now.minusSeconds(5))).block();

		approvalStore.save(approval("approval-old", "rec-old", "incident-old",
				RecommendationApprovalStatus.PENDING, now.minusSeconds(8 * 24 * 60 * 60L))).block();
		lifecycleStore.save(lifecycle("incident-old", IncidentStatus.OPEN,
				now.minusSeconds(8 * 24 * 60 * 60L))).block();

		GovernanceDashboardBacklogService service =
				new GovernanceDashboardBacklogService(
						approvalStore,
						executionPlanStore,
						executionResultStore,
						verificationResultStore,
						lifecycleStore,
						draftStore,
						learningCandidateStore,
						promotionPlanStore,
						knowledgeUpdateStore
				);

		GovernanceDashboardBacklogSummary summary = service.backlog(
				new GovernanceDashboardQuery(
						null,
						now.minusSeconds(24 * 60 * 60),
						now.plusSeconds(1)
				)
		).block();

		assertThat(summary.timeRange()).isNotNull();
		assertThat(summary.pendingRecommendationApprovals()).isEqualTo(1);
		assertThat(summary.approvedRecommendationsWithoutExecutionPlan()).isEqualTo(1);
		assertThat(summary.executionResultsAwaitingVerification()).isEqualTo(1);
		assertThat(summary.unresolvedIncidents()).isEqualTo(1);
		assertThat(summary.postmortemDraftsAwaitingReview()).isEqualTo(1);
		assertThat(summary.learningCandidatesAwaitingPromotionReview()).isEqualTo(1);
		assertThat(summary.promotionPlansAwaitingApplication()).isEqualTo(1);
		assertThat(summary.items())
				.extracting(GovernanceBacklogItem::category, GovernanceBacklogItem::count)
				.contains(
						org.assertj.core.groups.Tuple.tuple(
								"pendingRecommendationApprovals",
								1L
						),
						org.assertj.core.groups.Tuple.tuple(
								"promotionPlansAwaitingApplication",
								1L
						)
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
			Instant verifiedAt
	) {
		return new VerificationResultRecord(
				verificationId,
				executionResultId,
				executionPlanId,
				recommendationId,
				incidentId,
				VerificationStatus.VERIFIED,
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
				status == IncidentStatus.OPEN ? null : IncidentStatus.MITIGATING,
				status,
				IncidentTransitionReason.INCIDENT_RESOLVED,
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

	private LearningCandidateRecord learningCandidate(
			String candidateId,
			String incidentId,
			LearningCandidateStatus status,
			Instant createdAt
	) {
		return new LearningCandidateRecord(
				candidateId,
				incidentId,
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				status,
				"operator-a",
				"candidate",
				List.of(),
				createdAt,
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
