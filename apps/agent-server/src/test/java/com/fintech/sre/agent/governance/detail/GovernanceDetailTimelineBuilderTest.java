package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanTarget;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionTargetType;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

class GovernanceDetailTimelineBuilderTest {

	@Test
	void shouldBuildRecommendationTimelineInOccurredAtAscendingOrder() {
		GovernanceDetailTimelineBuilder builder =
				new GovernanceDetailTimelineBuilder(new GovernanceDetailSanitizer());
		Instant base = Instant.parse("2026-05-10T00:00:00Z");

		List<GovernanceDetailTimelineItem> timeline = builder.buildRecommendationTimeline(
				new RecommendationRecord(
						"rec-1",
						"incident-1",
						"audit-1",
						"PROMETHEUS_ALERTMANAGER",
						"payment-api",
						"payment",
						"CRITICAL",
						"firing",
						base.plusSeconds(20),
						1,
						0,
						"ALLOW",
						"PASS",
						List.of("RATE_LIMIT"),
						List.of(),
						Map.of()
				),
				List.of(new RecommendationApprovalRecord(
						"approval-1",
						"rec-1",
						"incident-1",
						RecommendationApprovalStatus.APPROVED,
						"operator-a",
						"approved",
						base.plusSeconds(10),
						Map.of()
				)),
				List.of(),
				List.of(),
				List.of()
		);

		assertThat(timeline).hasSize(2);
		assertThat(timeline.get(0).occurredAt()).isBefore(timeline.get(1).occurredAt());
		assertThat(timeline.get(0).type()).isEqualTo("APPROVAL_DECIDED");
		assertThat(timeline.get(1).type()).isEqualTo("RECOMMENDATION_CREATED");
	}

	@Test
	void shouldRedactSensitiveSummaryText() {
		GovernanceDetailTimelineBuilder builder =
				new GovernanceDetailTimelineBuilder(new GovernanceDetailSanitizer());

		List<GovernanceDetailTimelineItem> timeline = builder.buildIncidentTimeline(
				List.of(new IncidentLifecycleRecord(
						"lifecycle-1",
						"incident-1",
						IncidentStatus.OPEN,
						IncidentStatus.MITIGATING,
						IncidentTransitionReason.MANUAL_ESCALATION,
						"operator-a",
						"customer token leaked in rawLog",
						Instant.parse("2026-05-10T00:00:00Z"),
						Map.of()
				)),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);

		assertThat(timeline.get(0).summary()).isEqualTo("[redacted]");
	}

	@Test
	void shouldBuildLearningTimelineInOccurredAtAscendingOrder() {
		GovernanceDetailTimelineBuilder builder =
				new GovernanceDetailTimelineBuilder(new GovernanceDetailSanitizer());
		Instant base = Instant.parse("2026-05-10T00:00:00Z");

		List<GovernanceDetailTimelineItem> timeline = builder.buildLearningTimeline(
				new LearningCandidateRecord(
						"candidate-1",
						"incident-1",
						"draft-1",
						"review-1",
						LearningCandidateType.RUNBOOK_UPDATE,
						LearningCandidateStatus.REVIEW_REQUIRED,
						"operator-a",
						"candidate",
						List.of(),
						base.plusSeconds(10),
						Map.of()
				),
				List.of(new KnowledgePromotionReviewRecord(
						"promotion-review-1",
						"candidate-1",
						"incident-1",
						KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
						"reviewer-a",
						"reviewed",
						"ok",
						base.plusSeconds(20),
						Map.of()
				)),
				List.of(new KnowledgePromotionPlanRecord(
						"promotion-plan-1",
						"candidate-1",
						"incident-1",
						KnowledgePromotionPlanStatus.PLAN_CREATED,
						"planner-a",
						"plan",
						List.of(new KnowledgePromotionPlanTarget(
								KnowledgePromotionTargetType.RUNBOOK,
								"runbooks/payment.md",
								"summary",
								List.of(),
								List.of()
						)),
						List.of(),
						List.of(),
						base.plusSeconds(30),
						Map.of()
				)),
				List.of(new KnowledgeUpdateApplicationRecord(
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
						List.of(),
						base.plusSeconds(40),
						Map.of()
				))
		);

		assertThat(timeline).hasSize(4);
		assertThat(timeline).isSortedAccordingTo(
				java.util.Comparator.comparing(GovernanceDetailTimelineItem::occurredAt)
		);
		assertThat(timeline.get(0).type()).isEqualTo("LEARNING_CANDIDATE_CREATED");
		assertThat(timeline.get(1).type()).isEqualTo("PROMOTION_REVIEWED");
		assertThat(timeline.get(2).type()).isEqualTo("PROMOTION_PLAN_CREATED");
		assertThat(timeline.get(3).type()).isEqualTo("KNOWLEDGE_UPDATED");
	}

	@Test
	void shouldRedactKnowledgeUpdateSummaryInLearningTimeline() {
		GovernanceDetailTimelineBuilder builder =
				new GovernanceDetailTimelineBuilder(new GovernanceDetailSanitizer());

		List<GovernanceDetailTimelineItem> timeline = builder.buildLearningTimeline(
				null,
				List.of(),
				List.of(),
				List.of(new KnowledgeUpdateApplicationRecord(
						"update-1",
						"incident-1",
						"candidate-1",
						"promotion-plan-1",
						"RUNBOOK",
						KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
						"payment payload path",
						KnowledgeUpdateChangeType.UPDATED,
						"portfolio",
						"main",
						"abc123",
						"PR-101",
						"operator-a",
						"reviewer-a",
						"approver-a",
						List.of(),
						Instant.parse("2026-05-11T00:00:00Z"),
						Map.of()
				))
		);

		assertThat(timeline).hasSize(1);
		assertThat(timeline.get(0).summary()).isEqualTo("[redacted]");
	}
}
