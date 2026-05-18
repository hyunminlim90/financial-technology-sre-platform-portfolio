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
import com.fintech.sre.agent.learning.candidate.InMemoryLearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.plan.InMemoryKnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanTarget;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionTargetType;
import com.fintech.sre.agent.postmortem.review.InMemoryPostmortemReviewStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.verification.InMemoryVerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

class GovernanceDashboardRiskServiceTest {

	@Test
	void shouldCalculateRiskIndicatorsWithinTimeRange() {
		Instant now = Instant.now();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		InMemoryVerificationResultStore verificationStore =
				new InMemoryVerificationResultStore();
		InMemoryIncidentLifecycleStore incidentLifecycleStore =
				new InMemoryIncidentLifecycleStore();
		InMemoryLearningCandidateStore learningCandidateStore =
				new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionPlanStore promotionPlanStore =
				new InMemoryKnowledgePromotionPlanStore();
		InMemoryPostmortemReviewStore postmortemReviewStore =
				new InMemoryPostmortemReviewStore();

		approvalStore.save(approval("approval-1", RecommendationApprovalStatus.REJECTED,
				now.minusSeconds(60))).block();
		approvalStore.save(approval("approval-2", RecommendationApprovalStatus.REJECTED,
				now.minusSeconds(55))).block();
		approvalStore.save(approval("approval-3", RecommendationApprovalStatus.APPROVED,
				now.minusSeconds(50))).block();

		verificationStore.save(verification("verification-1", VerificationStatus.NOT_VERIFIED,
				now.minusSeconds(45))).block();
		verificationStore.save(verification("verification-2", VerificationStatus.REGRESSION_DETECTED,
				now.minusSeconds(40))).block();
		verificationStore.save(verification("verification-3", VerificationStatus.VERIFIED,
				now.minusSeconds(35))).block();

		incidentLifecycleStore.save(lifecycle("incident-1", IncidentStatus.REOPENED,
				now.minusSeconds(30))).block();
		incidentLifecycleStore.save(lifecycle("incident-2", IncidentStatus.ESCALATED,
				now.minusSeconds(25))).block();
		incidentLifecycleStore.save(lifecycle("incident-3", IncidentStatus.RESOLVED,
				now.minusSeconds(20))).block();

		for (int i = 0; i < 11; i++) {
			learningCandidateStore.save(learningCandidate(
					"candidate-" + i,
					LearningCandidateStatus.REVIEW_REQUIRED,
					now.minusSeconds(15 - Math.min(i, 14))
			)).block();
		}

		for (int i = 0; i < 6; i++) {
			promotionPlanStore.save(promotionPlan(
					"plan-" + i,
					now.minusSeconds(10 - Math.min(i, 9))
			)).block();
		}

		postmortemReviewStore.save(postmortemReview("review-1", PostmortemReviewStatus.NEEDS_REVISION,
				now.minusSeconds(12))).block();
		postmortemReviewStore.save(postmortemReview("review-2", PostmortemReviewStatus.APPROVED,
				now.minusSeconds(11))).block();

		approvalStore.save(approval("approval-old", RecommendationApprovalStatus.REJECTED,
				now.minusSeconds(8 * 24 * 60 * 60L))).block();

		GovernanceDashboardRiskService service =
				new GovernanceDashboardRiskService(
						approvalStore,
						verificationStore,
						incidentLifecycleStore,
						learningCandidateStore,
						promotionPlanStore,
						postmortemReviewStore
				);

		GovernanceDashboardRiskSummary summary = service.summary(
				new GovernanceDashboardQuery(
						null,
						now.minusSeconds(24 * 60 * 60),
						now.plusSeconds(1)
				)
		).block();

		assertThat(summary.overallRiskLevel())
				.isEqualTo(GovernanceRiskLevel.CRITICAL);
		assertThat(summary.indicators())
				.anySatisfy(indicator -> assertThat(indicator.name())
						.isEqualTo("verificationFailureRate"));
		assertThat(summary.indicators())
				.anySatisfy(indicator -> {
					assertThat(indicator.name()).isEqualTo("approvalRejectRate");
					assertThat(indicator.level()).isEqualTo(GovernanceRiskLevel.CRITICAL);
				});
		assertThat(summary.indicators())
				.anySatisfy(indicator -> {
					assertThat(indicator.name()).isEqualTo("learningBacklog");
					assertThat(indicator.level()).isEqualTo(GovernanceRiskLevel.MEDIUM);
				});
	}

	private RecommendationApprovalRecord approval(
			String approvalId,
			RecommendationApprovalStatus status,
			Instant decidedAt
	) {
		return new RecommendationApprovalRecord(
				approvalId,
				"rec-" + approvalId,
				"incident-" + approvalId,
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
				IncidentStatus.MITIGATING,
				status,
				IncidentTransitionReason.INCIDENT_RESOLVED,
				"operator-a",
				"lifecycle",
				transitionedAt,
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

	private KnowledgePromotionPlanRecord promotionPlan(
			String planId,
			Instant createdAt
	) {
		return new KnowledgePromotionPlanRecord(
				planId,
				"candidate-" + planId,
				"incident-" + planId,
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
}
