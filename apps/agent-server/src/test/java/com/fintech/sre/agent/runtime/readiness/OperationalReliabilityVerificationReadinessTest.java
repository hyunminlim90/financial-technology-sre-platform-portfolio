package com.fintech.sre.agent.runtime.readiness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

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
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingRejectionReason;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingDecision;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioReference;
import com.fintech.sre.agent.runtime.reliability.VerificationReference;

class OperationalReliabilityVerificationReadinessTest {

	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";

	private final VerificationReadinessEvaluator evaluator =
			new VerificationReadinessEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		VerificationReadiness readiness = evaluator.evaluate(
				readyApprovalReadiness(),
				true
		);

		assertThat(readiness.readOnly()).isTrue();
		assertThat(readiness.actualVerificationExecution()).isFalse();
		assertThat(readiness.verificationRequestGeneration()).isFalse();
		assertThat(readiness.verificationWorkflow()).isFalse();
		assertThat(readiness.verificationReportGeneration()).isFalse();
		assertThat(readiness.executionPermission()).isFalse();
		assertThat(readiness.actionAdmission()).isFalse();
	}

	@Test
	void shouldBeReadyWhenApprovalReadinessIsReadyAndBindingsExist() {
		VerificationReadiness readiness = evaluator.evaluate(
				readyApprovalReadiness(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.READY);
		assertThat(readiness.reason()).isEqualTo(VerificationReadinessReason.READY_APPROVAL);
		assertThat(readiness.scope()).isEqualTo(VerificationReadinessScope.RUNTIME_READINESS);
	}

	@Test
	void shouldBlockWhenApprovalReadinessIsBlocked() {
		VerificationReadiness readiness = evaluator.evaluate(
				blockedApprovalReadiness(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.BLOCKED);
		assertThat(readiness.reason()).isEqualTo(VerificationReadinessReason.BLOCKED_APPROVAL);
	}

	@Test
	void shouldBeUnreliableWhenApprovalReadinessIsUnreliable() {
		VerificationReadiness readiness = evaluator.evaluate(
				unreliableApprovalReadiness(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.UNRELIABLE);
		assertThat(readiness.reason()).isEqualTo(VerificationReadinessReason.UNRELIABLE_APPROVAL);
	}

	@Test
	void shouldBeNotReadyWhenApprovalReadinessIsNotReady() {
		VerificationReadiness readiness = evaluator.evaluate(
				notReadyApprovalReadiness(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.NOT_READY);
		assertThat(readiness.reason()).isEqualTo(VerificationReadinessReason.NOT_READY_APPROVAL);
	}

	@Test
	void shouldBePartialWhenApprovalReadinessIsPartial() {
		VerificationReadiness readiness = evaluator.evaluate(
				partialApprovalReadiness(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.PARTIAL);
		assertThat(readiness.reason()).isEqualTo(VerificationReadinessReason.PARTIAL_APPROVAL);
	}

	@Test
	void shouldBlockWhenVerificationBindingMissing() {
		VerificationReadiness readiness = evaluator.evaluate(
				readyApprovalReadinessWithMissingVerificationBinding(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(VerificationReadinessReason.MISSING_VERIFICATION_BINDING);
		assertThat(readiness.scope())
				.isEqualTo(VerificationReadinessScope.VERIFICATION_BOUNDARY);
	}

	@Test
	void shouldBlockWhenVerificationEvidenceRequirementMissing() {
		VerificationReadiness readiness = evaluator.evaluate(
				readyApprovalReadiness(),
				false
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.BLOCKED);
		assertThat(readiness.reason()).isEqualTo(
				VerificationReadinessReason.MISSING_VERIFICATION_EVIDENCE_REQUIREMENT
		);
		assertThat(readiness.scope())
				.isEqualTo(VerificationReadinessScope.VERIFICATION_EVIDENCE);
	}

	@Test
	void shouldBlockWhenRollbackBindingMissing() {
		VerificationReadiness readiness = evaluator.evaluate(
				readyApprovalReadinessWithMissingRollbackBinding(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(VerificationReadinessReason.MISSING_ROLLBACK_BINDING);
		assertThat(readiness.scope()).isEqualTo(VerificationReadinessScope.ROLLBACK_BOUNDARY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		VerificationReadiness readiness = evaluator.evaluate(
				readyApprovalReadinessWithCriticalRisk(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(VerificationReadinessReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(readiness.scope()).isEqualTo(VerificationReadinessScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		VerificationReadiness readiness = evaluator.evaluate(
				paymentApprovalReadiness(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(VerificationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(readiness.scope()).isEqualTo(VerificationReadinessScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBePartialWhenLifecycleUncertaintyExists() {
		VerificationReadiness readiness = evaluator.evaluate(
				readyApprovalReadinessWithUncertainty(),
				true
		);

		assertThat(readiness.level()).isEqualTo(VerificationReadinessLevel.PARTIAL);
		assertThat(readiness.reason())
				.isEqualTo(VerificationReadinessReason.LIFECYCLE_UNCERTAINTY);
		assertThat(readiness.scope()).isEqualTo(
				VerificationReadinessScope.LIFECYCLE_UNCERTAINTY
		);
	}

	@Test
	void shouldRejectNullApprovalReadiness() {
		assertThatThrownBy(() -> evaluator.evaluate(null, true))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("approvalReadiness must not be null");
	}

	private ApprovalReadiness readyApprovalReadiness() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				readyRecommendationReadiness(boundRollbackVerification()),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness readyApprovalReadinessWithCriticalRisk() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				new RecommendationReadiness(
						RecommendationReadinessLevel.READY,
						RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
						RecommendationReadinessScope.RUNTIME_READINESS,
						highRecommendationReliability(boundRollbackVerification()),
						OperationalUncertainty.CRITICAL,
						false
				),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness readyApprovalReadinessWithUncertainty() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				new RecommendationReadiness(
						RecommendationReadinessLevel.READY,
						RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
						RecommendationReadinessScope.RUNTIME_READINESS,
						highRecommendationReliability(boundRollbackVerification()),
						OperationalUncertainty.LOW,
						true
				),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness readyApprovalReadinessWithMissingVerificationBinding() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				readyRecommendationReadiness(missingVerificationBinding()),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness readyApprovalReadinessWithMissingRollbackBinding() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				readyRecommendationReadiness(missingRollbackBinding()),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness partialApprovalReadiness() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.PARTIAL,
				ApprovalReadinessReason.PARTIAL_RECOMMENDATION,
				ApprovalReadinessScope.OPERATOR_VIEW,
				partialRecommendationReadiness(boundRollbackVerification()),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness notReadyApprovalReadiness() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.NOT_READY,
				ApprovalReadinessReason.NOT_READY_RECOMMENDATION,
				ApprovalReadinessScope.OPERATOR_VIEW,
				notReadyRecommendationReadiness(boundRollbackVerification()),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness unreliableApprovalReadiness() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.UNRELIABLE,
				ApprovalReadinessReason.UNRELIABLE_RECOMMENDATION,
				ApprovalReadinessScope.RECOMMENDATION_READINESS,
				unreliableRecommendationReadiness(boundRollbackVerification()),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness blockedApprovalReadiness() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.BLOCKED,
				ApprovalReadinessReason.BLOCKED_RECOMMENDATION,
				ApprovalReadinessScope.RECOMMENDATION_READINESS,
				blockedRecommendationReadiness(boundRollbackVerification()),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness paymentApprovalReadiness() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.BLOCKED,
				ApprovalReadinessReason.PAYMENT_SAFETY_UNCERTAINTY,
				ApprovalReadinessScope.PAYMENT_SAFETY,
				paymentRecommendationReadiness(boundRollbackVerification()),
				OPERATOR_CONTEXT
		);
	}

	private RecommendationReadiness readyRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.READY,
				RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RUNTIME_READINESS,
				highRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness partialRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.PARTIAL,
				RecommendationReadinessReason.MEDIUM_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.OPERATOR_VIEW,
				mediumRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness notReadyRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.NOT_READY,
				RecommendationReadinessReason.LOW_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.OPERATOR_VIEW,
				lowRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness unreliableRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.UNRELIABLE,
				RecommendationReadinessReason.UNRELIABLE_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RECOMMENDATION_RELIABILITY,
				unreliableRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness blockedRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.BLOCKED,
				RecommendationReadinessReason.BLOCKED_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RECOMMENDATION_RELIABILITY,
				blockedRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness paymentRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.BLOCKED,
				RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY,
				RecommendationReadinessScope.PAYMENT_SAFETY,
				paymentRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReliability highRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY,
				bindingDecision
		);
	}

	private RecommendationReliability mediumRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.MEDIUM,
				RecommendationReliabilityReason.UNKNOWN,
				bindingDecision
		);
	}

	private RecommendationReliability lowRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.LOW,
				RecommendationReliabilityReason.LOW_DECISION_RELIABILITY,
				bindingDecision
		);
	}

	private RecommendationReliability unreliableRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.UNRELIABLE,
				RecommendationReliabilityReason.UNRELIABLE_DECISION,
				bindingDecision
		);
	}

	private RecommendationReliability blockedRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.BLOCKED,
				RecommendationReliabilityReason.BLOCKED_DECISION,
				bindingDecision
		);
	}

	private RecommendationReliability paymentRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				bindingDecision
		);
	}

	private RecommendationReliability recommendationReliability(
			RecommendationReliabilityLevel level,
			RecommendationReliabilityReason reason,
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReliability(
				level,
				reason,
				level == RecommendationReliabilityLevel.MEDIUM
						? RecommendationReliabilityScope.OPERATOR_VIEW
						: reason == RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY
						? RecommendationReliabilityScope.PAYMENT_SAFETY
						: RecommendationReliabilityScope.DECISION,
				decisionReliability(bindingDecision),
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

	private DecisionReliability decisionReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new DecisionReliability(
				DecisionReliabilityLevel.HIGH,
				DecisionReliabilityReason.HIGH_ASSESSMENT_RELIABILITY,
				DecisionReliabilityScope.ASSESSMENT,
				assessmentReliability(),
				boundScenario(),
				bindingDecision
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
						Instant.parse("2026-06-13T00:00:00Z"),
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

	private RollbackVerificationBindingDecision missingVerificationBinding() {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.REJECTED,
				new RollbackReference(
						"rollback/payments",
						"rollback-knowledge",
						true,
						false
				),
				null,
				RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE
		);
	}

	private RollbackVerificationBindingDecision missingRollbackBinding() {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.REJECTED,
				null,
				new VerificationReference(
						"verification/payments",
						"verification-knowledge",
						true,
						false,
						false
				),
				RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE
		);
	}
}
