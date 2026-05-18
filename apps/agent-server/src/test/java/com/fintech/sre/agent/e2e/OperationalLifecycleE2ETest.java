package com.fintech.sre.agent.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleService;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleTransitionRequest;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleTransitionResponse;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRequest;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationResponse;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationService;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;
import com.fintech.sre.agent.learning.candidate.LearningCandidatePromotionRequest;
import com.fintech.sre.agent.learning.candidate.LearningCandidatePromotionResponse;
import com.fintech.sre.agent.learning.candidate.LearningCandidatePromotionService;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRequest;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanResponse;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanService;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRequest;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewResponse;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewService;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRequest;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftResponse;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftService;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRequest;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewResponse;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewService;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalDecision;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRequest;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalResponse;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalService;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.audit.RecommendationApprovalAuditLogger;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanRequest;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanResponse;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlanService;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRequest;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultResponse;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultService;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRequest;
import com.fintech.sre.agent.recommendation.verification.VerificationResultResponse;
import com.fintech.sre.agent.recommendation.verification.VerificationResultService;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

@SpringBootTest
class OperationalLifecycleE2ETest {

	@Autowired
	private RecommendationRecordStore recommendationRecordStore;

	@Autowired
	private RecommendationApprovalService recommendationApprovalService;

	@Autowired
	private RecommendationApprovalStore recommendationApprovalStore;

	@Autowired
	private RecommendationApprovalAuditLogger recommendationApprovalAuditLogger;

	@Autowired
	private RecommendationExecutionPlanService recommendationExecutionPlanService;

	@Autowired
	private HumanExecutionResultService humanExecutionResultService;

	@Autowired
	private HumanExecutionResultStore humanExecutionResultStore;

	@Autowired
	private VerificationResultService verificationResultService;

	@Autowired
	private VerificationResultStore verificationResultStore;

	@Autowired
	private IncidentLifecycleService incidentLifecycleService;

	@Autowired
	private IncidentLifecycleStore incidentLifecycleStore;

	@Autowired
	private PostmortemDraftService postmortemDraftService;

	@Autowired
	private PostmortemDraftStore postmortemDraftStore;

	@Autowired
	private PostmortemReviewService postmortemReviewService;

	@Autowired
	private LearningCandidatePromotionService learningCandidatePromotionService;

	@Autowired
	private LearningCandidateStore learningCandidateStore;

	@Autowired
	private KnowledgePromotionReviewService knowledgePromotionReviewService;

	@Autowired
	private KnowledgePromotionPlanService knowledgePromotionPlanService;

	@Autowired
	private KnowledgeUpdateApplicationService knowledgeUpdateApplicationService;

	@Autowired
	private KnowledgeUpdateApplicationStore knowledgeUpdateApplicationStore;

	@Test
	void should_complete_full_operational_learning_lifecycle() {
		String suffix = UUID.randomUUID().toString();
		String incidentId = "incident-e2e-" + suffix;
		String recommendationId = "recommendation-e2e-" + suffix;

		RecommendationRecord recommendation = recommendationRecordStore.save(
				new RecommendationRecord(
						recommendationId,
						incidentId,
						"audit-" + suffix,
						"PROMETHEUS_ALERTMANAGER",
						"payment-api",
						"payment",
						"CRITICAL",
						"firing",
						Instant.now(),
						1,
						0,
						"ALLOW",
						"PASS",
						List.of("RATE_LIMIT"),
						List.of(),
						Map.of(
								"alertName", "HighP99Latency",
								"service", "payment-api",
								"domain", "payment"
						)
				)
		).block();

		assertThat(recommendation.recommendationRecordId()).isNotBlank();

		RecommendationApprovalResponse approval = recommendationApprovalService.decide(
				recommendationId,
				new RecommendationApprovalRequest(
						RecommendationApprovalDecision.APPROVED,
						"operator-a",
						"Runbook and evidence match the incident.",
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		assertThat(approval.status())
				.isEqualTo(RecommendationApprovalStatus.APPROVED);

		RecommendationApprovalRecord approvalRecord =
				recommendationApprovalStore.findLatestByRecommendationRecordId(recommendationId)
						.block();
		assertThat(approvalRecord.metadata())
				.containsKey("team")
				.doesNotContainKey("paymentPayload");
		assertThat(recommendationApprovalAuditLogger.findByIncidentId(incidentId).collectList().block())
				.hasSize(1);

		ExecutionPlanResponse executionPlan = recommendationExecutionPlanService.createDryRunPlan(
				recommendationId,
				new ExecutionPlanRequest(
						"operator-a",
						"Create dry-run execution plan for final human review."
				)
		).block();

		assertThat(executionPlan.status())
				.isEqualTo(ExecutionPlanStatus.DRY_RUN_PLAN_CREATED);
		assertThat(executionPlan.executable())
				.isFalse();
		assertThat(executionPlan.steps()).isNotEmpty();

		HumanExecutionResultResponse executionResult = humanExecutionResultService.record(
				executionPlan.executionPlanId(),
				new HumanExecutionResultRequest(
						HumanExecutionStatus.EXECUTED,
						"operator-a",
						"Manual rate-limit action applied outside agent-server.",
						Instant.now().minusSeconds(120),
						Instant.now().minusSeconds(60),
						Map.of(
								"ticket", "INC-" + suffix,
								"rawLog", "must-not-store",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		assertThat(executionResult.status())
				.isEqualTo(HumanExecutionStatus.EXECUTED);

		HumanExecutionResultRecord executionRecord =
				humanExecutionResultStore.findById(executionResult.executionResultId()).block();
		assertThat(executionRecord.metadata())
				.containsKey("ticket")
				.doesNotContainKey("paymentPayload")
				.doesNotContainKey("rawLog")
				.doesNotContainKey("kubectl");

		VerificationResultResponse verification = verificationResultService.verify(
				executionResult.executionResultId(),
				new VerificationResultRequest(
						VerificationStatus.VERIFIED,
						"operator-a",
						"Latency normalized and error rate returned to baseline.",
						Map.of(
								"dashboard", "latency",
								"customerToken", "must-not-store"
						)
				)
		).block();

		assertThat(verification.status())
				.isEqualTo(VerificationStatus.VERIFIED);

		VerificationResultRecord verificationRecord =
				verificationResultStore.findById(verification.verificationResultId()).block();
		assertThat(verificationRecord.metadata())
				.containsKey("dashboard")
				.doesNotContainKey("customerToken");

		incidentLifecycleService.transition(
				incidentId,
				new IncidentLifecycleTransitionRequest(
						IncidentStatus.OPEN,
						IncidentTransitionReason.ALERT_RECEIVED,
						"operator-a",
						"Alert received and incident opened.",
						Map.of("channel", "alertmanager")
				)
		).block();

		incidentLifecycleService.transition(
				incidentId,
				new IncidentLifecycleTransitionRequest(
						IncidentStatus.MITIGATING,
						IncidentTransitionReason.MITIGATION_IN_PROGRESS,
						"operator-a",
						"Mitigation in progress.",
						Map.of("owner", "sre")
				)
		).block();

		incidentLifecycleService.transition(
				incidentId,
				new IncidentLifecycleTransitionRequest(
						IncidentStatus.STABILIZING,
						IncidentTransitionReason.STABILIZATION_WINDOW_STARTED,
						"operator-a",
						"Stabilization window started after manual action.",
						Map.of("window", "15m")
				)
		).block();

		IncidentLifecycleTransitionResponse resolvedTransition =
				incidentLifecycleService.transition(
						incidentId,
						new IncidentLifecycleTransitionRequest(
								IncidentStatus.RESOLVED,
								IncidentTransitionReason.INCIDENT_RESOLVED,
								"operator-a",
								"Incident resolved after verification.",
								Map.of(
										"resolvedBy", "operator-a",
										"rawLog", "must-not-store"
								)
						)
				).block();

		assertThat(resolvedTransition.currentStatus())
				.isEqualTo(IncidentStatus.RESOLVED);

		IncidentLifecycleRecord latestLifecycle =
				incidentLifecycleService.latest(incidentId).block();
		assertThat(latestLifecycle.currentStatus())
				.isEqualTo(IncidentStatus.RESOLVED);
		assertThat(incidentLifecycleStore.findByIncidentId(incidentId).collectList().block())
				.hasSize(4);
		assertThat(latestLifecycle.metadata())
				.containsKey("resolvedBy")
				.doesNotContainKey("rawLog");

		PostmortemDraftResponse draft = postmortemDraftService.create(
				incidentId,
				new PostmortemDraftRequest(
						"operator-a",
						"Prepare human-review postmortem draft.",
						Map.of(
								"documentOwner", "sre",
								"prompt", "must-not-store",
								"rawLog", "must-not-store"
						)
				)
		).block();

		assertThat(draft.status())
				.isEqualTo(PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED);
		assertThat(draft.summary())
				.contains("does not assert root cause certainty");

		PostmortemDraftRecord draftRecord =
				postmortemDraftStore.findById(draft.postmortemDraftId()).block();
		assertThat(draftRecord.metadata())
				.containsKey("documentOwner")
				.doesNotContainKey("prompt")
				.doesNotContainKey("rawLog");

		PostmortemReviewResponse review = postmortemReviewService.review(
				draft.postmortemDraftId(),
				new PostmortemReviewRequest(
						PostmortemReviewStatus.APPROVED,
						"reviewer-a",
						"Evidence reviewed.",
						"Approved for learning candidate promotion.",
						Map.of(
								"customerToken", "must-not-store",
								"reviewChannel", "postmortem-review"
						)
				)
		).block();

		assertThat(review.status())
				.isEqualTo(PostmortemReviewStatus.APPROVED);
		assertThat(postmortemDraftStore.findById(draft.postmortemDraftId()).block().status())
				.isEqualTo(PostmortemDraftStatus.APPROVED);

		LearningCandidatePromotionResponse candidate = learningCandidatePromotionService.promote(
				draft.postmortemDraftId(),
				new LearningCandidatePromotionRequest(
						LearningCandidateType.RUNBOOK_UPDATE,
						"operator-a",
						"Promote approved operational learning into runbook update candidate.",
						List.of(
								"Add latency verification checklist to the runbook.",
								"payment payload dump"
						),
						Map.of(
								"domain", "payment",
								"service", "payment-api",
								"rawLog", "must-not-store"
						)
				)
		).block();

		assertThat(candidate.status())
				.isEqualTo(LearningCandidateStatus.REVIEW_REQUIRED);

		LearningCandidateRecord candidateRecord =
				learningCandidateStore.findById(candidate.learningCandidateId()).block();
		assertThat(candidateRecord.proposedChanges())
				.contains("Add latency verification checklist to the runbook.")
				.doesNotContain("payment payload dump");
		assertThat(candidateRecord.metadata())
				.containsEntry("domain", "payment")
				.containsEntry("service", "payment-api")
				.doesNotContainKey("rawLog");

		KnowledgePromotionReviewResponse promotionReview =
				knowledgePromotionReviewService.review(
						candidate.learningCandidateId(),
						new KnowledgePromotionReviewRequest(
								KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
								"reviewer-b",
								"Ready for promotion planning.",
								"Approved for human-managed knowledge promotion planning.",
								Map.of(
										"team", "sre",
										"paymentPayload", "must-not-store"
								)
						)
				).block();

		assertThat(promotionReview.status())
				.isEqualTo(KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION);

		KnowledgePromotionPlanResponse promotionPlan =
				knowledgePromotionPlanService.createPlan(
						candidate.learningCandidateId(),
						new KnowledgePromotionPlanRequest(
								"planner-a",
								"Create a manual runbook update plan.",
								Map.of(
										"team", "sre",
										"rawLog", "must-not-store"
								)
						)
				).block();

		assertThat(promotionPlan.status())
				.isEqualTo(KnowledgePromotionPlanStatus.PLAN_CREATED);
		assertThat(promotionPlan.targets()).isNotEmpty();
		assertThat(promotionPlan.targets().get(0).recommendedPath())
				.contains("runbooks");
		assertThat(promotionPlan.requiredHumanChecks())
				.anyMatch(check -> check.contains("outside agent-server"));

		KnowledgePromotionPlanRecord promotionPlanRecord =
				knowledgePromotionPlanService.findById(promotionPlan.promotionPlanId()).block();
		assertThat(promotionPlanRecord.metadata())
				.containsKey("team")
				.doesNotContainKey("rawLog");

		KnowledgeUpdateApplicationResponse knowledgeUpdate =
				knowledgeUpdateApplicationService.apply(
						candidate.learningCandidateId(),
						new KnowledgeUpdateApplicationRequest(
								promotionPlan.promotionPlanId(),
								"RUNBOOK",
								KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
								promotionPlan.targets().get(0).recommendedPath(),
								KnowledgeUpdateChangeType.UPDATED,
								"fin-tech-sre-platform-portfolio",
								"main",
								"a1b2c3d4",
								"PR-101",
								"operator-a",
								"reviewer-b",
								"approver-c",
								List.of(
										"Runbook markdown lint passed.",
										"customer payload verified"
								),
								Map.of(
										"team", "sre",
										"paymentPayload", "must-not-store"
								)
						)
				).block();

		assertThat(knowledgeUpdate.gitCommitSha())
				.isEqualTo("a1b2c3d4");
		assertThat(knowledgeUpdate.filePath())
				.contains("runbooks");

		KnowledgeUpdateApplicationRecord updateRecord =
				knowledgeUpdateApplicationStore.findById(
						knowledgeUpdate.knowledgeUpdateApplicationId()
				).block();
		assertThat(updateRecord.validationChecks())
				.doesNotContain("customer payload verified");
		assertThat(updateRecord.metadata())
				.containsKey("team")
				.doesNotContainKey("paymentPayload")
				.doesNotContainKey("gitPushResult")
				.doesNotContainKey("qdrantUpsert");

		assertThat(recommendationApprovalStore.findByIncidentId(incidentId).collectList().block())
				.hasSize(1);
		assertThat(humanExecutionResultStore.findByIncidentId(incidentId).collectList().block())
				.hasSize(1);
		assertThat(verificationResultStore.findByIncidentId(incidentId).collectList().block())
				.hasSize(1);
		assertThat(learningCandidateStore.findByIncidentId(incidentId).collectList().block())
				.hasSize(1);
		assertThat(knowledgeUpdateApplicationStore.findByIncidentId(incidentId).collectList().block())
				.hasSize(1);
	}
}
