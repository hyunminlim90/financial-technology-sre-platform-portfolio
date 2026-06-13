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
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageStatus;
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

class OperationalReliabilityRecommendationReadinessTest {

	private final RecommendationReadinessEvaluator evaluator =
			new RecommendationReadinessEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		RecommendationReadiness readiness = evaluator.evaluate(
				highRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);

		assertThat(readiness.readOnly()).isTrue();
		assertThat(readiness.recommendationGeneration()).isFalse();
		assertThat(readiness.operatorExposure()).isFalse();
		assertThat(readiness.approvalRequest()).isFalse();
		assertThat(readiness.executionPermission()).isFalse();
		assertThat(readiness.actionCommandGeneration()).isFalse();
	}

	@Test
	void shouldBeReadyForHighReliabilityWithoutRiskOrUncertainty() {
		RecommendationReadiness readiness = evaluator.evaluate(
				highRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);

		assertThat(readiness.level()).isEqualTo(RecommendationReadinessLevel.READY);
		assertThat(readiness.reason())
				.isEqualTo(RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY);
		assertThat(readiness.scope()).isEqualTo(RecommendationReadinessScope.RUNTIME_READINESS);
	}

	@Test
	void shouldBlockHighReliabilityWhenCriticalLifecycleRiskExists() {
		RecommendationReadiness readiness = evaluator.evaluate(
				highRecommendationReliability(),
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(readiness.level()).isEqualTo(RecommendationReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(RecommendationReadinessReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(readiness.scope()).isEqualTo(RecommendationReadinessScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldDowngradeHighReliabilityWithLifecycleUncertaintyToPartial() {
		RecommendationReadiness readiness = evaluator.evaluate(
				highRecommendationReliability(),
				OperationalUncertainty.LOW,
				true
		);

		assertThat(readiness.level()).isEqualTo(RecommendationReadinessLevel.PARTIAL);
		assertThat(readiness.reason())
				.isEqualTo(RecommendationReadinessReason.LIFECYCLE_UNCERTAINTY);
		assertThat(readiness.scope()).isEqualTo(
				RecommendationReadinessScope.LIFECYCLE_UNCERTAINTY
		);
	}

	@Test
	void shouldMarkMediumReliabilityAsPartial() {
		RecommendationReadiness readiness = evaluator.evaluate(
				mediumRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);

		assertThat(readiness.level()).isEqualTo(RecommendationReadinessLevel.PARTIAL);
		assertThat(readiness.reason())
				.isEqualTo(RecommendationReadinessReason.MEDIUM_RECOMMENDATION_RELIABILITY);
	}

	@Test
	void shouldMarkLowReliabilityAsNotReady() {
		RecommendationReadiness readiness = evaluator.evaluate(
				lowRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);

		assertThat(readiness.level()).isEqualTo(RecommendationReadinessLevel.NOT_READY);
		assertThat(readiness.reason())
				.isEqualTo(RecommendationReadinessReason.LOW_RECOMMENDATION_RELIABILITY);
	}

	@Test
	void shouldMarkUnreliableReliabilityAsUnreliable() {
		RecommendationReadiness readiness = evaluator.evaluate(
				unreliableRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);

		assertThat(readiness.level()).isEqualTo(RecommendationReadinessLevel.UNRELIABLE);
		assertThat(readiness.reason()).isEqualTo(
				RecommendationReadinessReason.UNRELIABLE_RECOMMENDATION_RELIABILITY
		);
	}

	@Test
	void shouldMarkBlockedReliabilityAsBlocked() {
		RecommendationReadiness readiness = evaluator.evaluate(
				blockedRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);

		assertThat(readiness.level()).isEqualTo(RecommendationReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(RecommendationReadinessReason.BLOCKED_RECOMMENDATION_RELIABILITY);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		RecommendationReadiness readiness = evaluator.evaluate(
				paymentRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);

		assertThat(readiness.level()).isEqualTo(RecommendationReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(readiness.scope()).isEqualTo(RecommendationReadinessScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldRejectNullRecommendationReliability() {
		assertThatThrownBy(() -> evaluator.evaluate(null, OperationalUncertainty.LOW, false))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("recommendationReliability must not be null");
	}

	@Test
	void shouldRejectNullLifecycleRisk() {
		assertThatThrownBy(() -> evaluator.evaluate(highRecommendationReliability(), null, false))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private RecommendationReliability highRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY
		);
	}

	private RecommendationReliability mediumRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.MEDIUM,
				RecommendationReliabilityReason.UNKNOWN
		);
	}

	private RecommendationReliability lowRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.LOW,
				RecommendationReliabilityReason.LOW_DECISION_RELIABILITY
		);
	}

	private RecommendationReliability unreliableRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.UNRELIABLE,
				RecommendationReliabilityReason.UNRELIABLE_DECISION
		);
	}

	private RecommendationReliability blockedRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.BLOCKED,
				RecommendationReliabilityReason.BLOCKED_DECISION
		);
	}

	private RecommendationReliability paymentRecommendationReliability() {
		return recommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY
		);
	}

	private RecommendationReliability recommendationReliability(
			RecommendationReliabilityLevel level,
			RecommendationReliabilityReason reason
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
				requiredHumanApproval()
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

	private HumanApprovalDecision requiredHumanApproval() {
		return new HumanApprovalDecision(
				true,
				HumanApprovalScope.REQUIRED,
				new HumanApprovalRequirement(true, true, false, false),
				List.of(HumanApprovalReason.HIGH_RISK_REQUIRES_HUMAN_APPROVAL)
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
