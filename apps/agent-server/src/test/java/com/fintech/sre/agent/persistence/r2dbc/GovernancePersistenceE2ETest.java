package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanTarget;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionTargetType;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStore;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStep;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("r2dbc")
@Testcontainers(disabledWithoutDocker = true)
class GovernancePersistenceE2ETest {

	@Container
	@SuppressWarnings("resource")
	static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>("postgres:16-alpine")
					.withDatabaseName("fin_sre")
					.withUsername("fin_sre")
					.withPassword("fin_sre")
					.withInitScript("db/schema-governance.sql");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add(
				"spring.r2dbc.url",
				() -> "r2dbc:postgresql://"
						+ POSTGRES.getHost()
						+ ":"
						+ POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
						+ "/"
						+ POSTGRES.getDatabaseName()
		);
		registry.add("spring.r2dbc.username", POSTGRES::getUsername);
		registry.add("spring.r2dbc.password", POSTGRES::getPassword);
	}

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RecommendationRecordStore recommendationRecordStore;

	@Autowired
	private RecommendationApprovalStore recommendationApprovalStore;

	@Autowired
	private ExecutionPlanStore executionPlanStore;

	@Autowired
	private HumanExecutionResultStore humanExecutionResultStore;

	@Autowired
	private VerificationResultStore verificationResultStore;

	@Autowired
	private IncidentLifecycleStore incidentLifecycleStore;

	@Autowired
	private PostmortemDraftStore postmortemDraftStore;

	@Autowired
	private PostmortemReviewStore postmortemReviewStore;

	@Autowired
	private LearningCandidateStore learningCandidateStore;

	@Autowired
	private KnowledgePromotionReviewStore knowledgePromotionReviewStore;

	@Autowired
	private KnowledgePromotionPlanStore knowledgePromotionPlanStore;

	@Autowired
	private KnowledgeUpdateApplicationStore knowledgeUpdateApplicationStore;

	@Test
	void shouldUseOnlyR2dbcGovernanceStoresWhenR2dbcProfileIsActive() {
		assertSingleStoreBean(
				RecommendationRecordStore.class,
				R2dbcRecommendationRecordStore.class
		);
		assertSingleStoreBean(
				RecommendationApprovalStore.class,
				R2dbcRecommendationApprovalStore.class
		);
		assertSingleStoreBean(ExecutionPlanStore.class, R2dbcExecutionPlanStore.class);
		assertSingleStoreBean(
				HumanExecutionResultStore.class,
				R2dbcHumanExecutionResultStore.class
		);
		assertSingleStoreBean(
				VerificationResultStore.class,
				R2dbcVerificationResultStore.class
		);
		assertSingleStoreBean(
				IncidentLifecycleStore.class,
				R2dbcIncidentLifecycleStore.class
		);
		assertSingleStoreBean(
				PostmortemDraftStore.class,
				R2dbcPostmortemDraftStore.class
		);
		assertSingleStoreBean(
				PostmortemReviewStore.class,
				R2dbcPostmortemReviewStore.class
		);
		assertSingleStoreBean(
				LearningCandidateStore.class,
				R2dbcLearningCandidateStore.class
		);
		assertSingleStoreBean(
				KnowledgePromotionReviewStore.class,
				R2dbcKnowledgePromotionReviewStore.class
		);
		assertSingleStoreBean(
				KnowledgePromotionPlanStore.class,
				R2dbcKnowledgePromotionPlanStore.class
		);
		assertSingleStoreBean(
				KnowledgeUpdateApplicationStore.class,
				R2dbcKnowledgeUpdateApplicationStore.class
		);
	}

	@Test
	void shouldPersistFullGovernanceLifecycleConsistentlyInPostgresql() {
		ScenarioData older = persistScenario("old", Instant.parse("2026-05-08T00:00:00Z"));
		ScenarioData newer = persistScenario("new", Instant.parse("2026-05-08T01:00:00Z"));

		RecommendationRecord storedRecommendation = requireValue(
				recommendationRecordStore.findById(
						newer.recommendationRecord().recommendationRecordId()
				).block()
		);
		assertThat(storedRecommendation.incidentId()).isEqualTo(newer.incidentId());
		assertThat(storedRecommendation.actionTypes())
				.containsExactly("ROLLING_RESTART", "CONFIG_RELOAD");
		assertThat(storedRecommendation.blockedReasons())
				.containsExactly("awaiting approval");
		assertThat(storedRecommendation.metadata())
				.containsEntry("sourceSystem", "alerts")
				.doesNotContainKey("paymentPayload");
		assertThat(recommendationRecordStore.findRecent(10).collectList().block())
				.extracting(RecommendationRecord::recommendationRecordId)
				.containsExactly(
						newer.recommendationRecord().recommendationRecordId(),
						older.recommendationRecord().recommendationRecordId()
				);

		RecommendationApprovalRecord latestApproval = requireValue(
				recommendationApprovalStore.findLatestByRecommendationRecordId(
						newer.recommendationRecord().recommendationRecordId()
				).block()
		);
		assertThat(latestApproval.approvalId())
				.isEqualTo(newer.recommendationApproval().approvalId());
		assertThat(latestApproval.status()).isEqualTo(RecommendationApprovalStatus.APPROVED);
		assertThat(latestApproval.metadata())
				.containsEntry("approvalChannel", "console")
				.doesNotContainKey("customerToken");
		assertThat(recommendationApprovalStore.findByIncidentId(newer.incidentId())
				.collectList()
				.block())
				.hasSize(2);
		assertThat(recommendationApprovalStore.findRecent(10).collectList().block())
				.extracting(RecommendationApprovalRecord::approvalId)
				.startsWith(newer.recommendationApproval().approvalId());

		RecommendationExecutionPlan storedPlan = requireValue(
				executionPlanStore.findById(
						newer.executionPlan().executionPlanId()
				).block()
		);
		assertThat(storedPlan.recommendationRecordId())
				.isEqualTo(newer.recommendationRecord().recommendationRecordId());
		assertThat(storedPlan.executable()).isFalse();
		assertThat(storedPlan.steps()).hasSize(2);
		assertThat(storedPlan.steps().get(0).parameters())
				.containsEntry("namespace", "payments");
		assertThat(storedPlan.blockedReasons())
				.containsExactly("human approval still required");
		assertThat(executionPlanStore.findByRecommendationRecordId(
				newer.recommendationRecord().recommendationRecordId()
		).collectList().block()).hasSize(1);
		assertThat(executionPlanStore.findRecent(10).collectList().block())
				.extracting(RecommendationExecutionPlan::executionPlanId)
				.containsExactly(
						newer.executionPlan().executionPlanId(),
						older.executionPlan().executionPlanId()
				);

		HumanExecutionResultRecord storedExecutionResult = requireValue(
				humanExecutionResultStore.findById(
						newer.humanExecutionResult().executionResultId()
				).block()
		);
		assertThat(storedExecutionResult.executionPlanId())
				.isEqualTo(newer.executionPlan().executionPlanId());
		assertThat(storedExecutionResult.status()).isEqualTo(HumanExecutionStatus.EXECUTED);
		assertThat(storedExecutionResult.metadata())
				.containsEntry("operatorNotes", "executed manually")
				.doesNotContainKey("secretToken");
		assertThat(humanExecutionResultStore.findByExecutionPlanId(
				newer.executionPlan().executionPlanId()
		).collectList().block()).hasSize(1);
		assertThat(humanExecutionResultStore.findByRecommendationRecordId(
				newer.recommendationRecord().recommendationRecordId()
		).collectList().block()).hasSize(1);
		assertThat(humanExecutionResultStore.findRecent(10).collectList().block())
				.extracting(HumanExecutionResultRecord::executionResultId)
				.containsExactly(
						newer.humanExecutionResult().executionResultId(),
						older.humanExecutionResult().executionResultId()
				);

		VerificationResultRecord storedVerification = requireValue(
				verificationResultStore.findById(
						newer.verificationResult().verificationResultId()
				).block()
		);
		assertThat(storedVerification.executionResultId())
				.isEqualTo(newer.humanExecutionResult().executionResultId());
		assertThat(storedVerification.status()).isEqualTo(VerificationStatus.VERIFIED);
		assertThat(storedVerification.metadata())
				.containsEntry("verificationSource", "dashboard")
				.doesNotContainKey("rawLog");
		assertThat(verificationResultStore.findByExecutionResultId(
				newer.humanExecutionResult().executionResultId()
		).collectList().block()).hasSize(1);
		assertThat(verificationResultStore.findByRecommendationRecordId(
				newer.recommendationRecord().recommendationRecordId()
		).collectList().block()).hasSize(1);
		assertThat(verificationResultStore.findRecent(10).collectList().block())
				.extracting(VerificationResultRecord::verificationResultId)
				.containsExactly(
						newer.verificationResult().verificationResultId(),
						older.verificationResult().verificationResultId()
				);

		IncidentLifecycleRecord latestLifecycle = requireValue(
				incidentLifecycleStore.findLatestByIncidentId(newer.incidentId()).block()
		);
		assertThat(latestLifecycle.currentStatus()).isEqualTo(IncidentStatus.RESOLVED);
		assertThat(latestLifecycle.transitionReason())
				.isEqualTo(IncidentTransitionReason.INCIDENT_RESOLVED);
		assertThat(latestLifecycle.metadata())
				.containsEntry("transitionChannel", "operator")
				.doesNotContainKey("passwordHint");
		assertThat(incidentLifecycleStore.findByIncidentId(newer.incidentId())
				.collectList()
				.block())
				.hasSize(2);
		assertThat(incidentLifecycleStore.findRecent(10).collectList().block())
				.extracting(IncidentLifecycleRecord::incidentLifecycleId)
				.containsSequence(
						newer.resolvedLifecycle().incidentLifecycleId(),
						newer.openLifecycle().incidentLifecycleId()
				);

		PostmortemDraftRecord storedDraft = requireValue(
				postmortemDraftStore.findById(
						newer.postmortemDraft().postmortemDraftId()
				).block()
		);
		assertThat(storedDraft.incidentId()).isEqualTo(newer.incidentId());
		assertThat(storedDraft.timeline())
				.containsExactly("alert opened", "manual mitigation executed");
		assertThat(storedDraft.learningCandidates())
				.containsExactly("update payment runbook");
		assertThat(storedDraft.metadata())
				.containsEntry("draftOwner", "sre")
				.doesNotContainKey("prompt");
		assertThat(postmortemDraftStore.findByIncidentId(newer.incidentId())
				.collectList()
				.block())
				.hasSize(1);
		assertThat(postmortemDraftStore.findRecent(10).collectList().block())
				.extracting(PostmortemDraftRecord::postmortemDraftId)
				.containsExactly(
						newer.postmortemDraft().postmortemDraftId(),
						older.postmortemDraft().postmortemDraftId()
				);

		PostmortemReviewRecord latestReview = requireValue(
				postmortemReviewStore.findLatestByDraftId(
						newer.postmortemDraft().postmortemDraftId()
				).block()
		);
		assertThat(latestReview.postmortemDraftId())
				.isEqualTo(newer.postmortemDraft().postmortemDraftId());
		assertThat(latestReview.status()).isEqualTo(PostmortemReviewStatus.APPROVED);
		assertThat(latestReview.metadata())
				.containsEntry("reviewChannel", "internal-console")
				.doesNotContainKey("secretValue");
		assertThat(postmortemReviewStore.findByDraftId(
				newer.postmortemDraft().postmortemDraftId()
		).collectList().block()).hasSize(1);
		assertThat(postmortemReviewStore.findByIncidentId(newer.incidentId())
				.collectList()
				.block())
				.hasSize(1);
		assertThat(postmortemReviewStore.findRecent(10).collectList().block())
				.extracting(PostmortemReviewRecord::postmortemReviewId)
				.containsExactly(
						newer.postmortemReview().postmortemReviewId(),
						older.postmortemReview().postmortemReviewId()
				);

		LearningCandidateRecord storedCandidate = requireValue(
				learningCandidateStore.findById(
						newer.learningCandidate().learningCandidateId()
				).block()
		);
		assertThat(storedCandidate.postmortemReviewId())
				.isEqualTo(newer.postmortemReview().postmortemReviewId());
		assertThat(storedCandidate.proposedChanges())
				.containsExactly("add rollback section", "document manual verification steps");
		assertThat(storedCandidate.metadata())
				.containsEntry("candidateOwner", "learning-team")
				.doesNotContainKey("paymentPayload");
		assertThat(learningCandidateStore.findByIncidentId(newer.incidentId())
				.collectList()
				.block())
				.hasSize(1);
		assertThat(learningCandidateStore.findRecent(10).collectList().block())
				.extracting(LearningCandidateRecord::learningCandidateId)
				.containsExactly(
						newer.learningCandidate().learningCandidateId(),
						older.learningCandidate().learningCandidateId()
				);

		KnowledgePromotionReviewRecord latestPromotionReview = requireValue(
				knowledgePromotionReviewStore.findLatestByLearningCandidateId(
						newer.learningCandidate().learningCandidateId()
				).block()
		);
		assertThat(latestPromotionReview.learningCandidateId())
				.isEqualTo(newer.learningCandidate().learningCandidateId());
		assertThat(latestPromotionReview.status())
				.isEqualTo(KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION);
		assertThat(latestPromotionReview.metadata())
				.containsEntry("reviewBoard", "sre-governance")
				.doesNotContainKey("customerId");
		assertThat(knowledgePromotionReviewStore.findByLearningCandidateId(
				newer.learningCandidate().learningCandidateId()
		).collectList().block()).hasSize(1);
		assertThat(knowledgePromotionReviewStore.findByIncidentId(newer.incidentId())
				.collectList()
				.block())
				.hasSize(1);
		assertThat(knowledgePromotionReviewStore.findRecent(10).collectList().block())
				.extracting(KnowledgePromotionReviewRecord::promotionReviewId)
				.containsExactly(
						newer.promotionReview().promotionReviewId(),
						older.promotionReview().promotionReviewId()
				);

		KnowledgePromotionPlanRecord storedPromotionPlan = requireValue(
				knowledgePromotionPlanStore.findById(
						newer.promotionPlan().promotionPlanId()
				).block()
		);
		assertThat(storedPromotionPlan.learningCandidateId())
				.isEqualTo(newer.learningCandidate().learningCandidateId());
		assertThat(storedPromotionPlan.targets()).hasSize(1);
		assertThat(storedPromotionPlan.targets().get(0).recommendedPath())
				.isEqualTo("runbooks/payments/payment-api-runbook.md");
		assertThat(storedPromotionPlan.requiredHumanChecks())
				.containsExactly("verify reviewer sign-off", "verify rollback instructions");
		assertThat(storedPromotionPlan.blockedReasons())
				.containsExactly("human repository update required");
		assertThat(storedPromotionPlan.metadata())
				.containsEntry("planOwner", "sre")
				.doesNotContainKey("rawLog");
		assertThat(knowledgePromotionPlanStore.findByLearningCandidateId(
				newer.learningCandidate().learningCandidateId()
		).collectList().block()).hasSize(1);
		assertThat(knowledgePromotionPlanStore.findByIncidentId(newer.incidentId())
				.collectList()
				.block())
				.hasSize(1);
		assertThat(knowledgePromotionPlanStore.findRecent(10).collectList().block())
				.extracting(KnowledgePromotionPlanRecord::promotionPlanId)
				.containsExactly(
						newer.promotionPlan().promotionPlanId(),
						older.promotionPlan().promotionPlanId()
				);

		KnowledgeUpdateApplicationRecord storedKnowledgeUpdate = requireValue(
				knowledgeUpdateApplicationStore.findById(
						newer.knowledgeUpdateApplication().knowledgeUpdateApplicationId()
				).block()
		);
		assertThat(storedKnowledgeUpdate.learningCandidateId())
				.isEqualTo(newer.learningCandidate().learningCandidateId());
		assertThat(storedKnowledgeUpdate.gitCommitSha()).isEqualTo("a1b2c3d4-new");
		assertThat(storedKnowledgeUpdate.filePath())
				.isEqualTo("runbooks/payments/payment-api-runbook.md");
		assertThat(storedKnowledgeUpdate.validationChecks())
				.containsExactly("rollback verification completed", "link to merged PR recorded")
				.doesNotContain("customer payload verified");
		assertThat(storedKnowledgeUpdate.metadata())
				.containsEntry("knowledgeOwner", "sre")
				.doesNotContainKey("prompt")
				.doesNotContainKey("gitPushResult")
				.doesNotContainKey("qdrantUpsert");
		assertThat(knowledgeUpdateApplicationStore.findByIncidentId(newer.incidentId())
				.collectList()
				.block())
				.hasSize(1);
		assertThat(knowledgeUpdateApplicationStore.findByLearningCandidateId(
				newer.learningCandidate().learningCandidateId()
		).collectList().block()).hasSize(1);
		assertThat(knowledgeUpdateApplicationStore.findRecent(10).collectList().block())
				.extracting(KnowledgeUpdateApplicationRecord::knowledgeUpdateApplicationId)
				.containsExactly(
						newer.knowledgeUpdateApplication().knowledgeUpdateApplicationId(),
						older.knowledgeUpdateApplication().knowledgeUpdateApplicationId()
				);
	}

	private ScenarioData persistScenario(String suffix, Instant anchor) {
		String incidentId = "incident-" + suffix;
		RecommendationRecord recommendationRecord = requireValue(
				recommendationRecordStore.save(new RecommendationRecord(
						"recommendation-record-" + suffix,
						incidentId,
						"audit-" + suffix,
						"alert-manager",
						"payment-api",
						"payments",
						"HIGH",
						"CREATED",
						anchor,
						2,
						0,
						"ALLOW",
						"ALLOW_WITH_GUARDRAIL",
						List.of("ROLLING_RESTART", "CONFIG_RELOAD"),
						List.of("awaiting approval"),
						Map.of(
								"sourceSystem", "alerts",
								"paymentPayload", "must-not-persist"
						)
				)).block()
		);

		requireValue(recommendationApprovalStore.save(new RecommendationApprovalRecord(
				"recommendation-approval-pending-" + suffix,
				recommendationRecord.recommendationRecordId(),
				incidentId,
				RecommendationApprovalStatus.PENDING,
				"operator-" + suffix,
				"awaiting final human check",
				anchor.plusSeconds(30),
				Map.of(
						"approvalChannel", "console",
						"customerToken", "must-not-persist"
				)
		)).block());

		RecommendationApprovalRecord recommendationApproval = requireValue(
				recommendationApprovalStore.save(new RecommendationApprovalRecord(
						"recommendation-approval-" + suffix,
						recommendationRecord.recommendationRecordId(),
						incidentId,
						RecommendationApprovalStatus.APPROVED,
						"operator-" + suffix,
						"manual review completed",
						anchor.plusSeconds(60),
						Map.of(
								"approvalChannel", "console",
								"customerToken", "must-not-persist"
						)
				)).block()
		);

		RecommendationExecutionPlan executionPlan = requireValue(
				executionPlanStore.save(new RecommendationExecutionPlan(
						"execution-plan-" + suffix,
						recommendationRecord.recommendationRecordId(),
						incidentId,
						ExecutionPlanStatus.DRY_RUN_PLAN_CREATED,
						false,
						true,
						"planner-" + suffix,
						"dry run only plan",
						anchor.plusSeconds(120),
						List.of(
								new ExecutionPlanStep(
										"ROLLING_RESTART",
										"payment-api",
										"KUBERNETES",
										"MEDIUM",
										"SINGLE_SERVICE",
										true,
										true,
										true,
										true,
										true,
										Map.of("namespace", "payments", "deployment", "payment-api")
								),
								new ExecutionPlanStep(
										"CONFIG_RELOAD",
										"payment-api",
										"APPLICATION",
										"LOW",
										"SINGLE_SERVICE",
										true,
										false,
										false,
										true,
										true,
										Map.of("configKey", "feature.toggle")
								)
						),
						List.of("human approval still required")
				)).block()
		);

		HumanExecutionResultRecord humanExecutionResult = requireValue(
				humanExecutionResultStore.save(new HumanExecutionResultRecord(
						"execution-result-" + suffix,
						executionPlan.executionPlanId(),
						recommendationRecord.recommendationRecordId(),
						incidentId,
						HumanExecutionStatus.EXECUTED,
						"operator-" + suffix,
						"operator executed the approved dry-run procedure manually",
						anchor.plusSeconds(180),
						anchor.plusSeconds(240),
						anchor.plusSeconds(300),
						Map.of(
								"operatorNotes", "executed manually",
								"secretToken", "must-not-persist"
						)
				)).block()
		);

		VerificationResultRecord verificationResult = requireValue(
				verificationResultStore.save(new VerificationResultRecord(
						"verification-result-" + suffix,
						humanExecutionResult.executionResultId(),
						executionPlan.executionPlanId(),
						recommendationRecord.recommendationRecordId(),
						incidentId,
						VerificationStatus.VERIFIED,
						"verifier-" + suffix,
						"service recovered and alert volume returned to normal",
						anchor.plusSeconds(360),
						Map.of(
								"verificationSource", "dashboard",
								"rawLog", "must-not-persist"
						)
				)).block()
		);

		IncidentLifecycleRecord openLifecycle = requireValue(
				incidentLifecycleStore.save(new IncidentLifecycleRecord(
						"incident-lifecycle-open-" + suffix,
						incidentId,
						null,
						IncidentStatus.OPEN,
						IncidentTransitionReason.ALERT_RECEIVED,
						"operator-" + suffix,
						"incident opened from alert",
						anchor.plusSeconds(420),
						Map.of(
								"transitionChannel", "operator",
								"passwordHint", "must-not-persist"
						)
				)).block()
		);

		IncidentLifecycleRecord resolvedLifecycle = requireValue(
				incidentLifecycleStore.save(new IncidentLifecycleRecord(
						"incident-lifecycle-resolved-" + suffix,
						incidentId,
						IncidentStatus.STABILIZING,
						IncidentStatus.RESOLVED,
						IncidentTransitionReason.INCIDENT_RESOLVED,
						"operator-" + suffix,
						"operator confirmed incident resolution",
						anchor.plusSeconds(480),
						Map.of(
								"transitionChannel", "operator",
								"passwordHint", "must-not-persist"
						)
				)).block()
		);

		PostmortemDraftRecord postmortemDraft = requireValue(
				postmortemDraftStore.save(new PostmortemDraftRecord(
						"postmortem-draft-" + suffix,
						incidentId,
						PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED,
						"sre-bot",
						"Draft does not assert root cause certainty.",
						List.of("alert opened", "manual mitigation executed"),
						List.of("keep rollback step in runbook"),
						List.of(humanExecutionResult.summary()),
						List.of(verificationResult.summary()),
						List.of("re-check deployment timeout"),
						List.of("update payment runbook"),
						List.of("what signal detects config drift earlier?"),
						anchor.plusSeconds(540),
						Map.of(
								"draftOwner", "sre",
								"prompt", "must-not-persist"
						)
				)).block()
		);

		PostmortemReviewRecord postmortemReview = requireValue(
				postmortemReviewStore.save(new PostmortemReviewRecord(
						"postmortem-review-" + suffix,
						postmortemDraft.postmortemDraftId(),
						incidentId,
						PostmortemReviewStatus.APPROVED,
						"reviewer-" + suffix,
						"learning content is accurate enough for promotion review",
						"Approved with manual follow-up on runbook quality.",
						anchor.plusSeconds(600),
						Map.of(
								"reviewChannel", "internal-console",
								"secretValue", "must-not-persist"
						)
				)).block()
		);

		LearningCandidateRecord learningCandidate = requireValue(
				learningCandidateStore.save(new LearningCandidateRecord(
						"learning-candidate-" + suffix,
						incidentId,
						postmortemDraft.postmortemDraftId(),
						postmortemReview.postmortemReviewId(),
						LearningCandidateType.RUNBOOK_UPDATE,
						LearningCandidateStatus.REVIEW_REQUIRED,
						"operator-" + suffix,
						"Candidate for payment runbook update.",
						List.of(
								"add rollback section",
								"document manual verification steps",
								"remove customer token example"
						),
						anchor.plusSeconds(660),
						Map.of(
								"candidateOwner", "learning-team",
								"paymentPayload", "must-not-persist"
						)
				)).block()
		);

		KnowledgePromotionReviewRecord promotionReview = requireValue(
				knowledgePromotionReviewStore.save(new KnowledgePromotionReviewRecord(
						"promotion-review-" + suffix,
						learningCandidate.learningCandidateId(),
						incidentId,
						KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
						"review-board-" + suffix,
						"runbook update is ready for a human repository change",
						"Approved for promotion after reviewer sign-off.",
						anchor.plusSeconds(720),
						Map.of(
								"reviewBoard", "sre-governance",
								"customerId", "must-not-persist"
						)
				)).block()
		);

		KnowledgePromotionPlanRecord promotionPlan = requireValue(
				knowledgePromotionPlanStore.save(new KnowledgePromotionPlanRecord(
						"promotion-plan-" + suffix,
						learningCandidate.learningCandidateId(),
						incidentId,
						KnowledgePromotionPlanStatus.PLAN_CREATED,
						"planner-" + suffix,
						"Human should update the payment runbook in the portfolio repo.",
						List.of(new KnowledgePromotionPlanTarget(
								KnowledgePromotionTargetType.RUNBOOK,
								"runbooks/payments/payment-api-runbook.md",
								"Add rollback and verification guidance.",
								List.of("add rollback section", "add verification checklist"),
								List.of("peer review", "link incident evidence")
						)),
						List.of(
								"verify reviewer sign-off",
								"verify rollback instructions"
						),
						List.of("human repository update required"),
						anchor.plusSeconds(780),
						Map.of(
								"planOwner", "sre",
								"rawLog", "must-not-persist"
						)
				)).block()
		);

		KnowledgeUpdateApplicationRecord knowledgeUpdateApplication = requireValue(
				knowledgeUpdateApplicationStore.save(new KnowledgeUpdateApplicationRecord(
						"knowledge-update-application-" + suffix,
						incidentId,
						learningCandidate.learningCandidateId(),
						promotionPlan.promotionPlanId(),
						"RUNBOOK",
						KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
						"runbooks/payments/payment-api-runbook.md",
						KnowledgeUpdateChangeType.UPDATED,
						"portfolio-repo",
						"main",
						"a1b2c3d4-" + suffix,
						"PR-" + suffix,
						"operator-" + suffix,
						"reviewer-" + suffix,
						"approver-" + suffix,
						List.of(
								"rollback verification completed",
								"link to merged PR recorded",
								"customer payload verified"
						),
						anchor.plusSeconds(840),
						Map.of(
								"knowledgeOwner", "sre",
								"prompt", "must-not-persist"
						)
				)).block()
		);

		return new ScenarioData(
				incidentId,
				recommendationRecord,
				recommendationApproval,
				executionPlan,
				humanExecutionResult,
				verificationResult,
				openLifecycle,
				resolvedLifecycle,
				postmortemDraft,
				postmortemReview,
				learningCandidate,
				promotionReview,
				promotionPlan,
				knowledgeUpdateApplication
		);
	}

	private <T> void assertSingleStoreBean(
			Class<T> storeType,
			Class<? extends T> expectedStoreClass
	) {
		Map<String, T> beans = applicationContext.getBeansOfType(storeType);
		assertThat(beans).hasSize(1);
		assertThat(beans.values().iterator().next()).isInstanceOf(expectedStoreClass);
	}

	private <T> T requireValue(T value) {
		assertThat(value).isNotNull();
		return value;
	}

	private record ScenarioData(
			String incidentId,
			RecommendationRecord recommendationRecord,
			RecommendationApprovalRecord recommendationApproval,
			RecommendationExecutionPlan executionPlan,
			HumanExecutionResultRecord humanExecutionResult,
			VerificationResultRecord verificationResult,
			IncidentLifecycleRecord openLifecycle,
			IncidentLifecycleRecord resolvedLifecycle,
			PostmortemDraftRecord postmortemDraft,
			PostmortemReviewRecord postmortemReview,
			LearningCandidateRecord learningCandidate,
			KnowledgePromotionReviewRecord promotionReview,
			KnowledgePromotionPlanRecord promotionPlan,
			KnowledgeUpdateApplicationRecord knowledgeUpdateApplication
	) {
	}
}
