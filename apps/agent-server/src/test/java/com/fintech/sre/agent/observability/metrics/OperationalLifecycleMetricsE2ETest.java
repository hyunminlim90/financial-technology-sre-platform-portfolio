package com.fintech.sre.agent.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.VerificationCommand;
import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.alert.AlertSeverity;
import com.fintech.sre.agent.alert.AlertSource;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleService;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleTransitionRequest;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRequest;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationService;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;
import com.fintech.sre.agent.learning.candidate.LearningCandidatePromotionRequest;
import com.fintech.sre.agent.learning.candidate.LearningCandidatePromotionResponse;
import com.fintech.sre.agent.learning.candidate.LearningCandidatePromotionService;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRequest;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanResponse;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanService;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRequest;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewService;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;
import com.fintech.sre.agent.model.common.ActionSource;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;
import com.fintech.sre.agent.model.response.PolicyDecisionView;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRequest;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftResponse;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftService;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRequest;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewService;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalDecision;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRequest;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalService;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanRequest;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanResponse;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlanService;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRequest;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultResponse;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultService;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.persistence.RecommendationPersistenceService;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRequest;
import com.fintech.sre.agent.recommendation.verification.VerificationResultService;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest
class OperationalLifecycleMetricsE2ETest {

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private RecommendationPersistenceService recommendationPersistenceService;

	@Autowired
	private RecommendationApprovalService recommendationApprovalService;

	@Autowired
	private RecommendationExecutionPlanService recommendationExecutionPlanService;

	@Autowired
	private HumanExecutionResultService humanExecutionResultService;

	@Autowired
	private VerificationResultService verificationResultService;

	@Autowired
	private IncidentLifecycleService incidentLifecycleService;

	@Autowired
	private PostmortemDraftService postmortemDraftService;

	@Autowired
	private PostmortemReviewService postmortemReviewService;

	@Autowired
	private LearningCandidatePromotionService learningCandidatePromotionService;

	@Autowired
	private KnowledgePromotionReviewService knowledgePromotionReviewService;

	@Autowired
	private KnowledgePromotionPlanService knowledgePromotionPlanService;

	@Autowired
	private KnowledgeUpdateApplicationService knowledgeUpdateApplicationService;

	@Test
	void should_record_governance_metrics_across_operational_lifecycle() {
		String suffix = UUID.randomUUID().toString();
		String incidentId = "incident-metrics-" + suffix;

		RecommendationRecord recommendation = recommendationPersistenceService.persist(
				"audit-" + suffix,
				List.of(alert(suffix)),
				List.of(response(incidentId))
		).blockFirst();

		recommendationApprovalService.decide(
				recommendation.recommendationRecordId(),
				new RecommendationApprovalRequest(
						RecommendationApprovalDecision.APPROVED,
						"operator-a",
						"approved",
						Map.of()
				)
		).block();

		ExecutionPlanResponse executionPlan = recommendationExecutionPlanService.createDryRunPlan(
				recommendation.recommendationRecordId(),
				new ExecutionPlanRequest("operator-a", "create plan")
		).block();

		HumanExecutionResultResponse executionResult = humanExecutionResultService.record(
				executionPlan.executionPlanId(),
				new HumanExecutionResultRequest(
						HumanExecutionStatus.EXECUTED,
						"operator-a",
						"manual action applied",
						Instant.now().minusSeconds(60),
						Instant.now(),
						Map.of()
				)
		).block();

		verificationResultService.verify(
				executionResult.executionResultId(),
				new VerificationResultRequest(
						VerificationStatus.VERIFIED,
						"operator-a",
						"recovered",
						Map.of()
				)
		).block();

		incidentLifecycleService.transition(
				incidentId,
				new IncidentLifecycleTransitionRequest(
						IncidentStatus.OPEN,
						IncidentTransitionReason.ALERT_RECEIVED,
						"operator-a",
						"opened",
						Map.of()
				)
		).block();
		incidentLifecycleService.transition(
				incidentId,
				new IncidentLifecycleTransitionRequest(
						IncidentStatus.MITIGATING,
						IncidentTransitionReason.MITIGATION_IN_PROGRESS,
						"operator-a",
						"mitigating",
						Map.of()
				)
		).block();
		incidentLifecycleService.transition(
				incidentId,
				new IncidentLifecycleTransitionRequest(
						IncidentStatus.STABILIZING,
						IncidentTransitionReason.STABILIZATION_WINDOW_STARTED,
						"operator-a",
						"stabilizing",
						Map.of()
				)
		).block();
		incidentLifecycleService.transition(
				incidentId,
				new IncidentLifecycleTransitionRequest(
						IncidentStatus.RESOLVED,
						IncidentTransitionReason.INCIDENT_RESOLVED,
						"operator-a",
						"resolved",
						Map.of()
				)
		).block();

		PostmortemDraftResponse draft = postmortemDraftService.create(
				incidentId,
				new PostmortemDraftRequest(
						"operator-a",
						"create draft",
						Map.of()
				)
		).block();

		postmortemReviewService.review(
				draft.postmortemDraftId(),
				new PostmortemReviewRequest(
						PostmortemReviewStatus.APPROVED,
						"reviewer-a",
						"reviewed",
						"approved",
						Map.of()
				)
		).block();

		LearningCandidatePromotionResponse candidate = learningCandidatePromotionService.promote(
				draft.postmortemDraftId(),
				new LearningCandidatePromotionRequest(
						LearningCandidateType.RUNBOOK_UPDATE,
						"operator-a",
						"candidate",
						List.of("Add verification checklist."),
						Map.of(
								"domain", "payment",
								"service", "payment-api"
						)
				)
		).block();

		knowledgePromotionReviewService.review(
				candidate.learningCandidateId(),
				new KnowledgePromotionReviewRequest(
						KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
						"reviewer-b",
						"reviewed",
						"approved for promotion",
						Map.of()
				)
		).block();

		KnowledgePromotionPlanResponse promotionPlan = knowledgePromotionPlanService.createPlan(
				candidate.learningCandidateId(),
				new KnowledgePromotionPlanRequest(
						"planner-a",
						"plan",
						Map.of(
								"domain", "payment",
								"service", "payment-api"
						)
				)
		).block();

		knowledgeUpdateApplicationService.apply(
				candidate.learningCandidateId(),
				new KnowledgeUpdateApplicationRequest(
						promotionPlan.promotionPlanId(),
						"RUNBOOK",
						KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
						promotionPlan.targets().get(0).recommendedPath(),
						KnowledgeUpdateChangeType.UPDATED,
						"portfolio",
						"main",
						"a1b2c3d4",
						"PR-101",
						"operator-a",
						"reviewer-b",
						"approver-c",
						List.of("markdown lint passed"),
						Map.of()
				)
		).block();

		assertCount(
				GovernanceMetricName.RECOMMENDATION_CREATED,
				Map.of(
						"service", "payment-api",
						"domain", "payment",
						"severity", "CRITICAL",
						"policyDecision", "ALLOW",
						"guardrailDecision", "PASS"
				)
		);
		assertCount(
				GovernanceMetricName.RECOMMENDATION_APPROVAL_DECISION,
				Map.of("status", "APPROVED", "incidentId", incidentId)
		);
		assertCount(
				GovernanceMetricName.EXECUTION_PLAN_CREATED,
				Map.of(
						"status", "DRY_RUN_PLAN_CREATED",
						"executable", "false",
						"requiresFinalApproval", "true"
				)
		);
		assertCount(
				GovernanceMetricName.HUMAN_EXECUTION_RESULT,
				Map.of("status", "EXECUTED", "incidentId", incidentId)
		);
		assertCount(
				GovernanceMetricName.VERIFICATION_RESULT,
				Map.of("status", "VERIFIED", "incidentId", incidentId)
		);
		assertCount(
				GovernanceMetricName.INCIDENT_LIFECYCLE_TRANSITION,
				Map.of(
						"from", "STABILIZING",
						"to", "RESOLVED",
						"reason", "INCIDENT_RESOLVED"
				)
		);
		assertCount(
				GovernanceMetricName.POSTMORTEM_DRAFT_CREATED,
				Map.of("status", "HUMAN_REVIEW_REQUIRED")
		);
		assertCount(
				GovernanceMetricName.POSTMORTEM_REVIEW_DECISION,
				Map.of("status", "APPROVED")
		);
		assertCount(
				GovernanceMetricName.LEARNING_CANDIDATE_CREATED,
				Map.of("type", "RUNBOOK_UPDATE", "status", "REVIEW_REQUIRED")
		);
		assertCount(
				GovernanceMetricName.KNOWLEDGE_PROMOTION_REVIEW,
				Map.of("status", "APPROVED_FOR_PROMOTION")
		);
		assertCount(
				GovernanceMetricName.KNOWLEDGE_PROMOTION_PLAN_CREATED,
				Map.of("status", "PLAN_CREATED")
		);
		assertCount(
				GovernanceMetricName.KNOWLEDGE_UPDATE_APPLIED,
				Map.of(
						"knowledgeType", "RUNBOOK",
						"knowledgeLayer", "PRIMARY_OPERATIONAL_KNOWLEDGE",
						"changeType", "UPDATED"
				)
		);
	}

	private void assertCount(String metricName, Map<String, String> tags) {
		Counter counter = meterRegistry.find(metricName)
				.tags(tags.entrySet().stream()
						.flatMap(entry -> java.util.stream.Stream.of(entry.getKey(), entry.getValue()))
						.toArray(String[]::new))
				.counter();

		assertThat(counter).isNotNull();
		assertThat(counter.count()).isGreaterThanOrEqualTo(1.0);
	}

	private AlertEvent alert(String suffix) {
		return new AlertEvent(
				"alert-" + suffix,
				AlertSource.PROMETHEUS_ALERTMANAGER,
				"HighP99Latency",
				AlertSeverity.CRITICAL,
				"firing",
				"payment-api",
				"payment",
				"payment",
				"p99 latency high",
				Instant.now().minusSeconds(60),
				null,
				Map.of(),
				Map.of()
		);
	}

	private IncidentRecommendationResponse response(String incidentId) {
		return new IncidentRecommendationResponse(
				incidentId,
				"firing",
				null,
				List.of(),
				null,
				List.of(new RecommendedAction(
						1,
						"Apply manual rate limit",
						new ActionCommand(
								"cmd-1",
								ActionType.RATE_LIMIT,
								new ActionTarget("payment", "payment-api", "deployment", "payment-api", "prod"),
								true,
								new RollbackCommand("Remove manual rate limit."),
								List.of(new VerificationCommand(
										"latency_p99",
										"< 500ms",
										"Verify latency recovery."
								))
						),
						"Reduce load",
						"HIGH",
						"Rollback available",
						List.of("Verify latency"),
						true,
						ActionSource.RUNBOOK
				)),
				List.of(),
				List.of(),
				null,
				true,
				null,
				new PolicyDecisionView("ALLOW", List.of()),
				List.of(),
				"PASS",
				null,
				null
		);
	}
}
