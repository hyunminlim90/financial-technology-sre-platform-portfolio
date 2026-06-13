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
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingDecision;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioReference;
import com.fintech.sre.agent.runtime.reliability.VerificationReference;

class OperationalReliabilityApprovalReadinessTest {

	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";

	private final ApprovalReadinessEvaluator evaluator =
			new ApprovalReadinessEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ApprovalReadiness readiness = evaluator.evaluate(
				readyRecommendationReadiness(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.readOnly()).isTrue();
		assertThat(readiness.approvalGeneration()).isFalse();
		assertThat(readiness.approvalRequestGeneration()).isFalse();
		assertThat(readiness.approvalWorkflow()).isFalse();
		assertThat(readiness.executionPermission()).isFalse();
		assertThat(readiness.actionAdmission()).isFalse();
	}

	@Test
	void shouldBeReadyWhenRecommendationReadinessIsReadyAndContextExists() {
		ApprovalReadiness readiness = evaluator.evaluate(
				readyRecommendationReadiness(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.READY);
		assertThat(readiness.reason()).isEqualTo(ApprovalReadinessReason.READY_RECOMMENDATION);
		assertThat(readiness.scope()).isEqualTo(ApprovalReadinessScope.RUNTIME_READINESS);
	}

	@Test
	void shouldBlockWhenRecommendationReadinessIsBlocked() {
		ApprovalReadiness readiness = evaluator.evaluate(
				blockedRecommendationReadiness(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ApprovalReadinessReason.BLOCKED_RECOMMENDATION);
	}

	@Test
	void shouldBeUnreliableWhenRecommendationReadinessIsUnreliable() {
		ApprovalReadiness readiness = evaluator.evaluate(
				unreliableRecommendationReadiness(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.UNRELIABLE);
		assertThat(readiness.reason())
				.isEqualTo(ApprovalReadinessReason.UNRELIABLE_RECOMMENDATION);
	}

	@Test
	void shouldBeNotReadyWhenRecommendationReadinessIsNotReady() {
		ApprovalReadiness readiness = evaluator.evaluate(
				notReadyRecommendationReadiness(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.NOT_READY);
		assertThat(readiness.reason())
				.isEqualTo(ApprovalReadinessReason.NOT_READY_RECOMMENDATION);
	}

	@Test
	void shouldBePartialWhenRecommendationReadinessIsPartial() {
		ApprovalReadiness readiness = evaluator.evaluate(
				partialRecommendationReadiness(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.PARTIAL);
		assertThat(readiness.reason())
				.isEqualTo(ApprovalReadinessReason.PARTIAL_RECOMMENDATION);
	}

	@Test
	void shouldBlockWhenOperatorContextMissing() {
		ApprovalReadiness readiness = evaluator.evaluate(
				readyRecommendationReadiness(),
				" "
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ApprovalReadinessReason.MISSING_OPERATOR_CONTEXT);
		assertThat(readiness.scope()).isEqualTo(ApprovalReadinessScope.OPERATOR_CONTEXT);
	}

	@Test
	void shouldBlockWhenHumanApprovalRequirementMissing() {
		ApprovalReadiness readiness = evaluator.evaluate(
				readyRecommendationReadinessWithoutApprovalRequirement(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ApprovalReadinessReason.MISSING_HUMAN_APPROVAL_REQUIREMENT);
		assertThat(readiness.scope()).isEqualTo(ApprovalReadinessScope.HUMAN_APPROVAL);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ApprovalReadiness readiness = evaluator.evaluate(
				readyRecommendationReadinessWithCriticalRisk(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ApprovalReadinessReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(readiness.scope()).isEqualTo(ApprovalReadinessScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ApprovalReadiness readiness = evaluator.evaluate(
				paymentRecommendationReadiness(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ApprovalReadinessReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(readiness.scope()).isEqualTo(ApprovalReadinessScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBePartialWhenLifecycleUncertaintyExists() {
		ApprovalReadiness readiness = evaluator.evaluate(
				readyRecommendationReadinessWithUncertainty(),
				OPERATOR_CONTEXT
		);

		assertThat(readiness.level()).isEqualTo(ApprovalReadinessLevel.PARTIAL);
		assertThat(readiness.reason())
				.isEqualTo(ApprovalReadinessReason.LIFECYCLE_UNCERTAINTY);
		assertThat(readiness.scope()).isEqualTo(
				ApprovalReadinessScope.LIFECYCLE_UNCERTAINTY
		);
	}

	@Test
	void shouldRejectNullRecommendationReadiness() {
		assertThatThrownBy(() -> evaluator.evaluate(null, OPERATOR_CONTEXT))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("recommendationReadiness must not be null");
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

	private RecommendationReadiness readyRecommendationReadinessWithCriticalRisk() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.READY,
				RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RUNTIME_READINESS,
				highRecommendationReliability(),
				OperationalUncertainty.CRITICAL,
				false
		);
	}

	private RecommendationReadiness readyRecommendationReadinessWithUncertainty() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.READY,
				RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RUNTIME_READINESS,
				highRecommendationReliability(),
				OperationalUncertainty.LOW,
				true
		);
	}

	private RecommendationReadiness readyRecommendationReadinessWithoutApprovalRequirement() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.READY,
				RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RUNTIME_READINESS,
				highRecommendationReliabilityWithoutApprovalRequirement(),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness partialRecommendationReadiness() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.PARTIAL,
				RecommendationReadinessReason.MEDIUM_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.OPERATOR_VIEW,
				mediumRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness notReadyRecommendationReadiness() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.NOT_READY,
				RecommendationReadinessReason.LOW_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.OPERATOR_VIEW,
				lowRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness unreliableRecommendationReadiness() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.UNRELIABLE,
				RecommendationReadinessReason.UNRELIABLE_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RECOMMENDATION_RELIABILITY,
				unreliableRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness blockedRecommendationReadiness() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.BLOCKED,
				RecommendationReadinessReason.BLOCKED_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RECOMMENDATION_RELIABILITY,
				blockedRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness paymentRecommendationReadiness() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.BLOCKED,
				RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY,
				RecommendationReadinessScope.PAYMENT_SAFETY,
				paymentRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReliability highRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability highRecommendationReliabilityWithoutApprovalRequirement() {
		return recommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY,
				approvalNotRequired()
		);
	}

	private RecommendationReliability mediumRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.MEDIUM,
				RecommendationReliabilityReason.UNKNOWN,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability lowRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.LOW,
				RecommendationReliabilityReason.LOW_DECISION_RELIABILITY,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability unreliableRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.UNRELIABLE,
				RecommendationReliabilityReason.UNRELIABLE_DECISION,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability blockedRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.BLOCKED,
				RecommendationReliabilityReason.BLOCKED_DECISION,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability paymentRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability recommendationReliability(
			RecommendationReliabilityLevel level,
			RecommendationReliabilityReason reason,
			HumanApprovalDecision humanApprovalDecision
	) {
		return new RecommendationReliability(
				level,
				reason,
				level == RecommendationReliabilityLevel.MEDIUM
						? RecommendationReliabilityScope.OPERATOR_VIEW
						: reason == RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY
						? RecommendationReliabilityScope.PAYMENT_SAFETY
						: RecommendationReliabilityScope.DECISION,
				decisionReliability(),
				humanApprovalDecision
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

	private HumanApprovalDecision approvalNotRequired() {
		return new HumanApprovalDecision(
				false,
				HumanApprovalScope.OPTIONAL,
				new HumanApprovalRequirement(false, false, false, false),
				List.of()
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
						Instant.parse("2026-06-12T00:00:00Z"),
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
