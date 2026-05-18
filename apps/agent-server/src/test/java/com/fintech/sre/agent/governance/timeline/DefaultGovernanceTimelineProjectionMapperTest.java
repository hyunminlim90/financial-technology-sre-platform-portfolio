package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.detail.GovernanceDetailSanitizer;
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
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

class DefaultGovernanceTimelineProjectionMapperTest {

	private final DefaultGovernanceTimelineProjectionMapper mapper =
			new DefaultGovernanceTimelineProjectionMapper(
					new GovernanceDetailSanitizer()
			);

	@Test
	void shouldProjectRecommendationRecord() {
		RecommendationRecord record = new RecommendationRecord(
				"rec-001",
				"incident-001",
				"audit-001",
				"ai-engine",
				"orders-api",
				"orders",
				"HIGH",
				"CREATED",
				Instant.parse("2026-05-13T00:00:00Z"),
				2,
				0,
				"ALLOW",
				"ALLOW",
				List.of("restart-service"),
				List.of("customer payload blocked"),
				Map.of(
						"notes", "prompt leak",
						"team", "sre"
				)
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.sourceType()).isEqualTo(
				GovernanceTimelineProjectionType.RECOMMENDATION_RECORD
		);
		assertThat(projection.sourceId()).isEqualTo("rec-001");
		assertThat(projection.incidentId()).isEqualTo("incident-001");
		assertThat(projection.event().eventId()).isEqualTo(
				"RECOMMENDATION_RECORD:rec-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.RECOMMENDATION_CREATED
		);
		assertThat(projection.event().actor().type()).isEqualTo(
				GovernanceTimelineActorType.AI
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.RECOMMENDATION
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.INFO
		);
		assertThat(projection.event().metadata().attributes())
				.containsEntry("source", "ai-engine")
				.containsEntry("service", "orders-api")
				.containsEntry("blockedReasons", "[redacted]")
				.containsEntry("notes", "[redacted]")
				.containsEntry("team", "sre");
	}

	@Test
	void shouldProjectApprovalRecord() {
		RecommendationApprovalRecord record = new RecommendationApprovalRecord(
				"approval-001",
				"rec-001",
				"incident-001",
				RecommendationApprovalStatus.REJECTED,
				"operator-1",
				"Needs revision due to token exposure",
				Instant.parse("2026-05-13T01:00:00Z"),
				Map.of("review", "manual")
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"APPROVAL_RECORD:approval-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.APPROVAL_DECIDED
		);
		assertThat(projection.event().actor()).isEqualTo(
				new GovernanceTimelineActor(
						GovernanceTimelineActorType.HUMAN,
						"operator-1",
						"Human Operator"
				)
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.APPROVAL
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.ERROR
		);
		assertThat(projection.event().metadata().attributes())
				.containsEntry("status", "REJECTED")
				.containsEntry("reason", "[redacted]")
				.containsEntry("review", "manual");
	}

	@Test
	void shouldProjectExecutionPlan() {
		RecommendationExecutionPlan record = new RecommendationExecutionPlan(
				"plan-001",
				"rec-001",
				"incident-001",
				ExecutionPlanStatus.BLOCKED,
				false,
				true,
				"system",
				"Blocked by secret rotation dependency",
				Instant.parse("2026-05-13T02:00:00Z"),
				List.of(),
				List.of("payment system dependency")
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo("EXECUTION_PLAN:plan-001");
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.EXECUTION_PLAN_CREATED
		);
		assertThat(projection.event().actor().type()).isEqualTo(
				GovernanceTimelineActorType.SYSTEM
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.EXECUTION_PLAN
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.WARNING
		);
		assertThat(projection.event().metadata().attributes())
				.containsEntry("blockedReasons", "[redacted]")
				.containsEntry("reason", "[redacted]")
				.containsEntry("requiresFinalApproval", "true");
	}

	@Test
	void shouldProjectVerificationResult() {
		VerificationResultRecord record = new VerificationResultRecord(
				"verification-001",
				"execution-result-001",
				"plan-001",
				"rec-001",
				"incident-001",
				VerificationStatus.REGRESSION_DETECTED,
				"operator-2",
				"Regression detected with customer token payload",
				Instant.parse("2026-05-13T03:00:00Z"),
				Map.of("notes", "rawLog attached")
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"VERIFICATION_RESULT:verification-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.VERIFICATION_RECORDED
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.ERROR
		);
		assertThat(projection.event().summary()).isEqualTo("[redacted]");
		assertThat(projection.event().metadata().attributes())
				.containsEntry("status", "REGRESSION_DETECTED")
				.containsEntry("notes", "[redacted]");
	}

	@Test
	void shouldProjectHumanExecutionResult() {
		HumanExecutionResultRecord record = new HumanExecutionResultRecord(
				"exec-001",
				"plan-001",
				"rec-001",
				"incident-001",
				HumanExecutionStatus.FAILED,
				"operator-4",
				"Execution failed due to secret token mismatch",
				Instant.parse("2026-05-13T02:30:00Z"),
				Instant.parse("2026-05-13T02:45:00Z"),
				Instant.parse("2026-05-13T02:50:00Z"),
				Map.of("notes", "payment payload captured")
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"HUMAN_EXECUTION_RESULT:exec-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.HUMAN_EXECUTION_RECORDED
		);
		assertThat(projection.event().actor().type()).isEqualTo(
				GovernanceTimelineActorType.HUMAN
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.HUMAN_EXECUTION
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.ERROR
		);
		assertThat(projection.event().summary()).isEqualTo("[redacted]");
		assertThat(projection.event().metadata().attributes())
				.containsEntry("status", "FAILED")
				.containsEntry("notes", "[redacted]");
	}

	@Test
	void shouldProjectIncidentLifecycle() {
		IncidentLifecycleRecord record = new IncidentLifecycleRecord(
				"lifecycle-001",
				"incident-001",
				IncidentStatus.MITIGATING,
				IncidentStatus.ESCALATED,
				IncidentTransitionReason.MANUAL_ESCALATION,
				"operator-3",
				"Escalated after payment rawLog review",
				Instant.parse("2026-05-13T04:00:00Z"),
				Map.of("channel", "war-room", "secretNote", "do not expose")
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"INCIDENT_LIFECYCLE:lifecycle-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.INCIDENT_TRANSITIONED
		);
		assertThat(projection.event().resource()).isEqualTo(
				new GovernanceTimelineResource(
						GovernanceTimelineResourceType.INCIDENT,
						"incident-001",
						"Incident incident-001"
				)
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.WARNING
		);
		assertThat(projection.event().summary()).isEqualTo("[redacted]");
		assertThat(projection.event().metadata().attributes())
				.containsEntry("currentStatus", "ESCALATED")
				.containsEntry("channel", "war-room")
				.containsEntry("[redacted]", "[redacted]");
	}

	@Test
	void shouldProjectPostmortemDraft() {
		PostmortemDraftRecord record = new PostmortemDraftRecord(
				"draft-001",
				"incident-001",
				PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED,
				"ai-agent",
				"Draft includes prompt and recovery notes",
				List.of("timeline-1"),
				List.of("recommendation-1"),
				List.of(),
				List.of(),
				List.of(),
				List.of("candidate-1"),
				List.of("customer payload question"),
				Instant.parse("2026-05-13T05:00:00Z"),
				Map.of("reviewHint", "secret follow-up")
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"POSTMORTEM_DRAFT:draft-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.POSTMORTEM_DRAFT_CREATED
		);
		assertThat(projection.event().actor().type()).isEqualTo(
				GovernanceTimelineActorType.AI
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.POSTMORTEM
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.WARNING
		);
		assertThat(projection.event().summary()).isEqualTo("[redacted]");
		assertThat(projection.event().metadata().attributes())
				.containsEntry("status", "HUMAN_REVIEW_REQUIRED")
				.containsEntry("openQuestions", "[redacted]")
				.containsEntry("reviewHint", "[redacted]");
	}

	@Test
	void shouldProjectPostmortemReview() {
		PostmortemReviewRecord record = new PostmortemReviewRecord(
				"review-001",
				"draft-001",
				"incident-001",
				PostmortemReviewStatus.NEEDS_REVISION,
				"operator-5",
				"Needs revision because of rawLog exposure",
				"Review found prompt leakage",
				Instant.parse("2026-05-13T06:00:00Z"),
				Map.of("note", "customer section too broad")
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"POSTMORTEM_REVIEW:review-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.POSTMORTEM_REVIEWED
		);
		assertThat(projection.event().actor().type()).isEqualTo(
				GovernanceTimelineActorType.HUMAN
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.POSTMORTEM
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.WARNING
		);
		assertThat(projection.event().summary()).isEqualTo("[redacted]");
		assertThat(projection.event().metadata().attributes())
				.containsEntry("reviewReason", "[redacted]")
				.containsEntry("note", "[redacted]");
	}

	@Test
	void shouldProjectLearningCandidate() {
		LearningCandidateRecord record = new LearningCandidateRecord(
				"learning-001",
				"incident-001",
				"draft-001",
				"review-001",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.REVIEW_REQUIRED,
				"system",
				"Learning candidate from payment prompt",
				List.of("update rawLog guidance", "token masking"),
				Instant.parse("2026-05-13T07:00:00Z"),
				Map.of("note", "customer-safe rewrite needed")
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"LEARNING_CANDIDATE:learning-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.LEARNING_CANDIDATE_CREATED
		);
		assertThat(projection.event().actor().type()).isEqualTo(
				GovernanceTimelineActorType.SYSTEM
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.LEARNING
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.WARNING
		);
		assertThat(projection.event().summary()).isEqualTo("[redacted]");
		assertThat(projection.event().metadata().attributes())
				.containsEntry("type", "RUNBOOK_UPDATE")
				.containsEntry("proposedChanges", "[redacted],[redacted]")
				.containsEntry("note", "[redacted]");
	}

	@Test
	void shouldProjectKnowledgePromotionReview() {
		KnowledgePromotionReviewRecord record =
				new KnowledgePromotionReviewRecord(
						"promo-review-001",
						"learning-001",
						"incident-001",
						KnowledgePromotionReviewStatus.NEEDS_REVISION,
						"operator-6",
						"Needs revision before promotion due to prompt exposure",
						"Review flagged rawLog details",
						Instant.parse("2026-05-13T08:00:00Z"),
						Map.of("note", "secret approval note")
				);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"KNOWLEDGE_PROMOTION_REVIEW:promo-review-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.PROMOTION_REVIEWED
		);
		assertThat(projection.event().actor().type()).isEqualTo(
				GovernanceTimelineActorType.HUMAN
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.KNOWLEDGE_PROMOTION
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.WARNING
		);
		assertThat(projection.event().summary()).isEqualTo("[redacted]");
		assertThat(projection.event().metadata().attributes())
				.containsEntry("reviewReason", "[redacted]")
				.containsEntry("note", "[redacted]");
	}

	@Test
	void shouldProjectKnowledgePromotionPlan() {
		KnowledgePromotionPlanRecord record = new KnowledgePromotionPlanRecord(
				"promo-plan-001",
				"learning-001",
				"incident-001",
				KnowledgePromotionPlanStatus.BLOCKED,
				"system",
				"Plan blocked by payment policy update",
				List.of(),
				List.of("human review"),
				List.of("secret dependency"),
				Instant.parse("2026-05-13T09:00:00Z"),
				Map.of("note", "prompt validation pending")
		);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"KNOWLEDGE_PROMOTION_PLAN:promo-plan-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.PROMOTION_PLAN_CREATED
		);
		assertThat(projection.event().actor().type()).isEqualTo(
				GovernanceTimelineActorType.SYSTEM
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.KNOWLEDGE_PROMOTION
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.WARNING
		);
		assertThat(projection.event().summary()).isEqualTo("[redacted]");
		assertThat(projection.event().metadata().attributes())
				.containsEntry("blockedReasons", "[redacted]")
				.containsEntry("requiredHumanChecks", "human review")
				.containsEntry("note", "[redacted]");
	}

	@Test
	void shouldProjectKnowledgeUpdateApplication() {
		KnowledgeUpdateApplicationRecord record =
				new KnowledgeUpdateApplicationRecord(
						"knowledge-001",
						"incident-001",
						"learning-001",
						"promo-plan-001",
						"runbook",
						KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
						"/docs/payment-runbook.md",
						KnowledgeUpdateChangeType.UPDATED,
						"git@example.com:repo.git",
						"main",
						"abcdef123456",
						"PR-101",
						"operator-7",
						"reviewer-1",
						"approver-1",
						List.of("prompt validation", "lint"),
						Instant.parse("2026-05-13T10:00:00Z"),
						Map.of("note", "customer content redacted")
				);

		GovernanceTimelineProjection projection = mapper.project(record).orElseThrow();

		assertThat(projection.event().eventId()).isEqualTo(
				"KNOWLEDGE_UPDATE_APPLICATION:knowledge-001"
		);
		assertThat(projection.event().eventType()).isEqualTo(
				GovernanceTimelineEventType.KNOWLEDGE_UPDATED
		);
		assertThat(projection.event().actor().type()).isEqualTo(
				GovernanceTimelineActorType.HUMAN
		);
		assertThat(projection.event().resource().type()).isEqualTo(
				GovernanceTimelineResourceType.KNOWLEDGE_UPDATE
		);
		assertThat(projection.event().severity()).isEqualTo(
				GovernanceTimelineSeverity.INFO
		);
		assertThat(projection.event().metadata().attributes())
				.containsEntry(
						"knowledgeLayer",
						"PRIMARY_OPERATIONAL_KNOWLEDGE"
				)
				.containsEntry("changeType", "UPDATED")
				.containsEntry("validationChecks", "[redacted],lint")
				.containsEntry("note", "[redacted]");
	}

	@Test
	void shouldReturnEmptyForNullOrUnknownSource() {
		Optional<GovernanceTimelineProjection> nullProjection = mapper.project(null);
		Optional<GovernanceTimelineProjection> unknownProjection =
				mapper.project(new Object());

		assertThat(nullProjection).isEmpty();
		assertThat(unknownProjection).isEmpty();
	}
}
