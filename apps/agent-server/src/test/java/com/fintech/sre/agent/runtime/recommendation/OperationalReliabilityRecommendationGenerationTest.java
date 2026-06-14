package com.fintech.sre.agent.runtime.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadiness;
import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadinessLevel;
import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadinessReason;
import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadinessScope;
import com.fintech.sre.agent.runtime.readiness.ApprovalReadiness;
import com.fintech.sre.agent.runtime.readiness.ApprovalReadinessLevel;
import com.fintech.sre.agent.runtime.readiness.ApprovalReadinessReason;
import com.fintech.sre.agent.runtime.readiness.ApprovalReadinessScope;
import com.fintech.sre.agent.runtime.readiness.RecommendationReadiness;
import com.fintech.sre.agent.runtime.readiness.RecommendationReadinessLevel;
import com.fintech.sre.agent.runtime.readiness.RecommendationReadinessReason;
import com.fintech.sre.agent.runtime.readiness.RecommendationReadinessScope;
import com.fintech.sre.agent.runtime.readiness.VerificationReadiness;
import com.fintech.sre.agent.runtime.readiness.VerificationReadinessLevel;
import com.fintech.sre.agent.runtime.readiness.VerificationReadinessReason;
import com.fintech.sre.agent.runtime.readiness.VerificationReadinessScope;
import com.fintech.sre.agent.runtime.reliability.AssessmentReliability;
import com.fintech.sre.agent.runtime.reliability.AssessmentReliabilityLevel;
import com.fintech.sre.agent.runtime.reliability.AssessmentReliabilityReason;
import com.fintech.sre.agent.runtime.reliability.AssessmentReliabilityScope;
import com.fintech.sre.agent.runtime.reliability.DecisionReliability;
import com.fintech.sre.agent.runtime.reliability.DecisionReliabilityLevel;
import com.fintech.sre.agent.runtime.reliability.DecisionReliabilityReason;
import com.fintech.sre.agent.runtime.reliability.DecisionReliabilityScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceClassification;
import com.fintech.sre.agent.runtime.reliability.EvidenceCompleteness;
import com.fintech.sre.agent.runtime.reliability.EvidenceConfidence;
import com.fintech.sre.agent.runtime.reliability.EvidenceConfidenceLevel;
import com.fintech.sre.agent.runtime.reliability.EvidenceConfidenceReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceConfidenceScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernanceIntegrationReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernanceIntegrationResult;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernanceIntegrationScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernanceIntegrationStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernancePolicy;
import com.fintech.sre.agent.runtime.reliability.EvidenceIntegrityStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineage;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageIntegrationReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageIntegrationResult;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageIntegrationScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageIntegrationStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceProvenance;
import com.fintech.sre.agent.runtime.reliability.EvidenceReliability;
import com.fintech.sre.agent.runtime.reliability.EvidenceReliabilityLevel;
import com.fintech.sre.agent.runtime.reliability.EvidenceReliabilityReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceReliabilityScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeApiRejectionReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeApiResponse;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeApiStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeSummaryReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeSummaryStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeSummaryView;
import com.fintech.sre.agent.runtime.reliability.EvidenceSourceType;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustLevel;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustScore;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustScoreLevel;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustScoreReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustScoreScope;
import com.fintech.sre.agent.runtime.reliability.HumanApprovalDecision;
import com.fintech.sre.agent.runtime.reliability.HumanApprovalReason;
import com.fintech.sre.agent.runtime.reliability.HumanApprovalRequirement;
import com.fintech.sre.agent.runtime.reliability.HumanApprovalScope;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliability;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliabilityLevel;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliabilityReason;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliabilityScope;
import com.fintech.sre.agent.runtime.reliability.RollbackReference;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingDecision;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingDecision;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioReference;
import com.fintech.sre.agent.runtime.reliability.VerificationReference;

class OperationalReliabilityRecommendationGenerationTest {

	private static final String ACTION_TYPE = "restart-deployment";
	private static final String BLAST_RADIUS_BOUNDARY = "namespace/payments-prod";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String RUNBOOK_BINDING = "runbook/payment-latency-mitigation";
	private static final Instant GENERATED_AT = Instant.parse("2026-06-14T00:00:00Z");

	private final RecommendationCandidateEvaluator candidateEvaluator =
			new RecommendationCandidateEvaluator();
	private final RecommendationContentIntegration contentIntegration =
			new RecommendationContentIntegration();
	private final RecommendationGenerationEvaluator evaluator =
			new RecommendationGenerationEvaluator();

	@Test
	void shouldBeGeneratableWhenContentReadyAndContentIsValid() {
		RecommendationGeneration generation = evaluate(contentReadyResult());

		assertThat(generation.level()).isEqualTo(
				RecommendationGenerationLevel.GENERATABLE
		);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.CONTENT_READY
		);
		assertThat(generation.scope()).isEqualTo(
				RecommendationGenerationScope.RECOMMENDATION_GENERATION
		);
	}

	@Test
	void shouldBlockWhenScenarioIdMissing() {
		RecommendationGeneration generation = evaluate(
				contentReadyResult(),
				false,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.BLOCKED);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.MISSING_SCENARIO_ID
		);
		assertThat(generation.scope()).isEqualTo(RecommendationGenerationScope.SCENARIO);
	}

	@Test
	void shouldBlockWhenRunbookIdMissing() {
		RecommendationGeneration generation = evaluate(
				contentReadyResult(),
				true,
				false,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.BLOCKED);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.MISSING_RUNBOOK_ID
		);
	}

	@Test
	void shouldBlockWhenRollbackIdMissing() {
		RecommendationGeneration generation = evaluate(
				contentReadyResult(),
				true,
				true,
				false,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.BLOCKED);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.MISSING_ROLLBACK_ID
		);
	}

	@Test
	void shouldBlockWhenVerificationIdMissing() {
		RecommendationGeneration generation = evaluate(
				contentReadyResult(),
				true,
				true,
				true,
				false,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.BLOCKED);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.MISSING_VERIFICATION_ID
		);
	}

	@Test
	void shouldBlockWhenPaymentSafetyClassificationMissing() {
		RecommendationGeneration generation = evaluate(
				contentReadyResult(),
				true,
				true,
				true,
				true,
				false,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.BLOCKED);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.MISSING_PAYMENT_SAFETY_CLASSIFICATION
		);
	}

	@Test
	void shouldBlockWhenPaymentSafetyClassificationInvalid() {
		RecommendationGeneration generation = evaluate(invalidPaymentClassificationResult());

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.BLOCKED);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.INVALID_PAYMENT_SAFETY_CLASSIFICATION
		);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		RecommendationGeneration generation = evaluate(
				contentReadyResult(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.BLOCKED);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(generation.scope()).isEqualTo(
				RecommendationGenerationScope.LIFECYCLE_RISK
		);
	}

	@Test
	void shouldBlockWhenPaymentSafetyIsUncertain() {
		RecommendationGeneration generation = evaluate(
				contentReadyResult(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.BLOCKED);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
	}

	@Test
	void shouldRemainPartialWhenContentIsPartial() {
		RecommendationGeneration generation = evaluate(partialContentResult());

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.PARTIAL);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.PARTIAL_CONTENT
		);
	}

	@Test
	void shouldRemainNotReadyWhenContentIsNotReady() {
		RecommendationGeneration generation = evaluate(notReadyContentResult());

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.NOT_READY);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.NOT_READY_CONTENT
		);
	}

	@Test
	void shouldRemainUnreliableWhenContentIsUnreliable() {
		RecommendationGeneration generation = evaluate(unreliableContentResult());

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.UNRELIABLE);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.UNRELIABLE_CONTENT
		);
	}

	@Test
	void shouldRemainBlockedWhenContentIsBlocked() {
		RecommendationGeneration generation = evaluate(blockedContentResult());

		assertThat(generation.level()).isEqualTo(RecommendationGenerationLevel.BLOCKED);
		assertThat(generation.reason()).isEqualTo(
				RecommendationGenerationReason.BLOCKED_CONTENT
		);
	}

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		RecommendationGeneration generation = evaluate(contentReadyResult());

		assertThat(generation.readOnly()).isTrue();
		assertThat(generation.recommendationEngine()).isFalse();
		assertThat(generation.llm()).isFalse();
		assertThat(generation.rag()).isFalse();
		assertThat(generation.runbookSelector()).isFalse();
		assertThat(generation.approvalRequest()).isFalse();
		assertThat(generation.actionCommand()).isFalse();
		assertThat(generation.executionPermission()).isFalse();
	}

	@Test
	void shouldRejectNullInputs() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("contentIntegrationResult must not be null");

		assertThatThrownBy(() -> evaluator.evaluate(
				contentReadyResult(),
				true,
				true,
				true,
				true,
				true,
				null,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private RecommendationGeneration evaluate(
			RecommendationContentIntegrationResult contentIntegrationResult
	) {
		return evaluate(
				contentIntegrationResult,
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationGeneration evaluate(
			RecommendationContentIntegrationResult contentIntegrationResult,
			boolean scenarioIdPresent,
			boolean runbookIdPresent,
			boolean rollbackIdPresent,
			boolean verificationIdPresent,
			boolean paymentSafetyClassificationPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		return evaluator.evaluate(
				contentIntegrationResult,
				scenarioIdPresent,
				runbookIdPresent,
				rollbackIdPresent,
				verificationIdPresent,
				paymentSafetyClassificationPresent,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private RecommendationContentIntegrationResult contentReadyResult() {
		return contentIntegration.integrate(
				validContent(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		);
	}

	private RecommendationContentIntegrationResult invalidPaymentClassificationResult() {
		RecommendationContent invalidContent = new RecommendationContentBuilder()
				.recommendationCandidate(eligibleCandidate())
				.recommendationId("rec-001")
				.title("Mitigate payment latency degradation")
				.summary("Use the matched runbook with rollback and verification references.")
				.recommendationType(RecommendationContentType.INCIDENT_RESPONSE)
				.reason(RecommendationContentReason.SCENARIO_MATCH)
				.scope(RecommendationContentScope.RECOMMENDATION_CONTENT)
				.scenarioId("payments-degradation")
				.runbookId(RUNBOOK_BINDING)
				.rollbackId("rollback/payments")
				.verificationId("verification/payments")
				.paymentSafetyClassification("UNKNOWN")
				.generatedAt(GENERATED_AT)
				.build();
		return contentIntegration.integrate(
				invalidContent,
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		);
	}

	private RecommendationContentIntegrationResult partialContentResult() {
		RecommendationContent partialContent = new RecommendationContentBuilder()
				.recommendationCandidate(eligibleCandidate())
				.recommendationId("rec-001")
				.title("Mitigate payment latency degradation")
				.summary("Use the matched runbook with rollback and verification references.")
				.recommendationType(RecommendationContentType.UNKNOWN)
				.reason(RecommendationContentReason.UNKNOWN)
				.scope(RecommendationContentScope.RECOMMENDATION_CONTENT)
				.scenarioId("payments-degradation")
				.runbookId(RUNBOOK_BINDING)
				.rollbackId("rollback/payments")
				.verificationId("verification/payments")
				.paymentSafetyClassification("PAYMENT_SAFE_REVIEWED")
				.generatedAt(GENERATED_AT)
				.build();
		return contentIntegration.integrate(
				partialContent,
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		);
	}

	private RecommendationContentIntegrationResult notReadyContentResult() {
		return new RecommendationContentIntegrationResult(
				validContent(),
				RecommendationContentIntegrationStatus.NOT_READY,
				RecommendationContentIntegrationReason.UNKNOWN,
				RecommendationContentIntegrationScope.OPERATOR_VIEW,
				false,
				false
		);
	}

	private RecommendationContentIntegrationResult unreliableContentResult() {
		return new RecommendationContentIntegrationResult(
				validContent(),
				RecommendationContentIntegrationStatus.UNRELIABLE,
				RecommendationContentIntegrationReason.UNKNOWN,
				RecommendationContentIntegrationScope.OPERATOR_VIEW,
				false,
				false
		);
	}

	private RecommendationContentIntegrationResult blockedContentResult() {
		return new RecommendationContentIntegrationResult(
				validContent(),
				RecommendationContentIntegrationStatus.BLOCKED,
				RecommendationContentIntegrationReason.UNKNOWN,
				RecommendationContentIntegrationScope.OPERATOR_VIEW,
				false,
				false
		);
	}

	private RecommendationContent validContent() {
		return new RecommendationContentBuilder()
				.recommendationCandidate(eligibleCandidate())
				.recommendationId("rec-001")
				.title("Mitigate payment latency degradation")
				.summary("Use the matched runbook with rollback and verification references.")
				.recommendationType(RecommendationContentType.INCIDENT_RESPONSE)
				.reason(RecommendationContentReason.SCENARIO_MATCH)
				.scope(RecommendationContentScope.RECOMMENDATION_CONTENT)
				.scenarioId("payments-degradation")
				.runbookId(RUNBOOK_BINDING)
				.rollbackId("rollback/payments")
				.verificationId("verification/payments")
				.paymentSafetyClassification("PAYMENT_SAFE_REVIEWED")
				.generatedAt(GENERATED_AT)
				.build();
	}

	private RecommendationCandidate eligibleCandidate() {
		return candidateEvaluator.evaluate(
				readyActionAdmissionReadiness(),
				RUNBOOK_BINDING
		);
	}

	private ActionAdmissionReadiness readyActionAdmissionReadiness() {
		return new ActionAdmissionReadiness(
				ActionAdmissionReadinessLevel.READY,
				ActionAdmissionReadinessReason.READY_VERIFICATION,
				ActionAdmissionReadinessScope.RUNTIME_READINESS,
				readyVerificationReadiness(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private VerificationReadiness readyVerificationReadiness() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				readyApprovalReadiness(),
				true
		);
	}

	private ApprovalReadiness readyApprovalReadiness() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				readyRecommendationReadiness(),
				OPERATOR_CONTEXT
		);
	}

	private RecommendationReadiness readyRecommendationReadiness() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.READY,
				RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RUNTIME_READINESS,
				highRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReliability highRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY,
				RecommendationReliabilityScope.DECISION,
				decisionReliability(),
				requiredHumanApproval()
		);
	}

	private HumanApprovalDecision requiredHumanApproval() {
		return new HumanApprovalDecision(
				true,
				HumanApprovalScope.REQUIRED,
				new HumanApprovalRequirement(true, true, false, false),
				List.of(HumanApprovalReason.HIGH_RISK_REQUIRES_HUMAN_APPROVAL)
		);
	}

	private DecisionReliability decisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.HIGH,
				DecisionReliabilityReason.HIGH_ASSESSMENT_RELIABILITY,
				DecisionReliabilityScope.ASSESSMENT,
				assessmentReliability(),
				boundScenario(),
				boundRollbackVerification()
		);
	}

	private AssessmentReliability assessmentReliability() {
		return new AssessmentReliability(
				AssessmentReliabilityLevel.HIGH,
				AssessmentReliabilityReason.HIGH_EVIDENCE_RELIABILITY,
				AssessmentReliabilityScope.ASSESSMENT,
				evidenceReliability(),
				true
		);
	}

	private EvidenceReliability evidenceReliability() {
		EvidenceGovernancePolicy governancePolicy = new EvidenceGovernancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.INTACT,
				EvidenceClassification.PUBLIC_SAFE,
				new EvidenceProvenance(
						EvidenceSourceType.METRICS,
						"adapter/prometheus",
						Instant.parse("2026-06-14T00:00:00Z"),
						true,
						false,
						false
				)
		);
		EvidenceLineage lineage = EvidenceLineage.trace(governancePolicy, true, true);
		EvidenceTrustScore trustScore = new EvidenceTrustScore(
				EvidenceTrustScoreLevel.HIGH,
				EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
				EvidenceTrustScoreScope.EVIDENCE,
				governanceResult(governancePolicy),
				lineageResult(lineage)
		);
		EvidenceConfidence confidence = new EvidenceConfidence(
				EvidenceConfidenceLevel.HIGH,
				EvidenceConfidenceReason.SUFFICIENT_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore
		);
		return new EvidenceReliability(
				EvidenceReliabilityLevel.HIGH,
				EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
				EvidenceReliabilityScope.EVIDENCE,
				governancePolicy,
				lineage,
				trustScore,
				confidence,
				true,
				false
		);
	}

	private EvidenceGovernanceIntegrationResult governanceResult(
			EvidenceGovernancePolicy governancePolicy
	) {
		return new EvidenceGovernanceIntegrationResult(
				governancePolicy,
				readableApiResponse(),
				EvidenceGovernanceIntegrationStatus.INTEGRATED,
				EvidenceGovernanceIntegrationReason.UNKNOWN,
				EvidenceGovernanceIntegrationScope.API_EXPOSURE
		);
	}

	private EvidenceLineageIntegrationResult lineageResult(EvidenceLineage lineage) {
		return new EvidenceLineageIntegrationResult(
				lineage,
				readableApiResponse(),
				EvidenceLineageIntegrationStatus.INTEGRATED,
				EvidenceLineageIntegrationReason.UNKNOWN,
				EvidenceLineageIntegrationScope.API_EXPOSURE
		);
	}

	private EvidenceRuntimeApiResponse readableApiResponse() {
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.HEALTHY,
						OperationalUncertainty.LOW,
						OperationalUncertainty.LOW,
						false,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						true,
						EvidenceCompleteness.COMPLETE
				),
				EvidenceRuntimeApiStatus.READABLE,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}

	private ScenarioBindingDecision boundScenario() {
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.BOUND,
				new ScenarioReference("payments-degradation", "scenario-knowledge", true, false),
				null
		);
	}

	private RollbackVerificationBindingDecision boundRollbackVerification() {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.BOUND,
				new RollbackReference(
						"rollback/payments",
						"rollback-knowledge",
						true,
						false
				),
				new VerificationReference(
						"verification/payments",
						"verification-knowledge",
						true,
						false,
						false
				),
				null
		);
	}
}
