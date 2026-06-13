package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRecommendationReliabilityTest {

	private final RecommendationReliabilityEvaluator evaluator =
			new RecommendationReliabilityEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		RecommendationReliability reliability = evaluator.evaluate(
				highDecisionReliability(),
				requiredHumanApproval()
		);

		assertThat(reliability.readOnly()).isTrue();
		assertThat(reliability.recommendation()).isFalse();
		assertThat(reliability.executionPermission()).isFalse();
		assertThat(reliability.actionAdmission()).isFalse();
		assertThat(reliability.actualRecommendation()).isFalse();
		assertThat(reliability.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBlockRecommendationForBlockedDecisionReliability() {
		RecommendationReliability reliability = evaluator.evaluate(
				blockedDecisionReliability(),
				requiredHumanApproval()
		);

		assertThat(reliability.level()).isEqualTo(RecommendationReliabilityLevel.BLOCKED);
		assertThat(reliability.reason()).isEqualTo(RecommendationReliabilityReason.BLOCKED_DECISION);
	}

	@Test
	void shouldMarkUnreliableDecisionAsUnreliableRecommendation() {
		RecommendationReliability reliability = evaluator.evaluate(
				unreliableDecisionReliability(),
				requiredHumanApproval()
		);

		assertThat(reliability.level()).isEqualTo(RecommendationReliabilityLevel.UNRELIABLE);
		assertThat(reliability.reason()).isEqualTo(RecommendationReliabilityReason.UNRELIABLE_DECISION);
	}

	@Test
	void shouldDowngradeLowDecisionReliability() {
		RecommendationReliability reliability = evaluator.evaluate(
				lowDecisionReliability(),
				requiredHumanApproval()
		);

		assertThat(reliability.level()).isEqualTo(RecommendationReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(RecommendationReliabilityReason.LOW_DECISION_RELIABILITY);
	}

	@Test
	void shouldBlockRecommendationWhenHumanApprovalRequirementMissing() {
		RecommendationReliability reliability = evaluator.evaluate(
				highDecisionReliability(),
				optionalHumanApproval()
		);

		assertThat(reliability.level()).isEqualTo(RecommendationReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(RecommendationReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT);
		assertThat(reliability.scope()).isEqualTo(RecommendationReliabilityScope.HUMAN_APPROVAL);
	}

	@Test
	void shouldBlockRecommendationWhenRollbackBindingMissing() {
		RecommendationReliability reliability = evaluator.evaluate(
				decisionReliabilityWithMissingRollback(),
				requiredHumanApproval()
		);

		assertThat(reliability.level()).isEqualTo(RecommendationReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(RecommendationReliabilityReason.MISSING_ROLLBACK_BINDING);
		assertThat(reliability.scope())
				.isEqualTo(RecommendationReliabilityScope.ROLLBACK_BOUNDARY);
	}

	@Test
	void shouldBlockRecommendationWhenVerificationBindingMissing() {
		RecommendationReliability reliability = evaluator.evaluate(
				decisionReliabilityWithMissingVerification(),
				requiredHumanApproval()
		);

		assertThat(reliability.level()).isEqualTo(RecommendationReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(RecommendationReliabilityReason.MISSING_VERIFICATION_BINDING);
		assertThat(reliability.scope())
				.isEqualTo(RecommendationReliabilityScope.VERIFICATION_BOUNDARY);
	}

	@Test
	void shouldDowngradeRecommendationForPaymentSafetyUncertainty() {
		RecommendationReliability reliability = evaluator.evaluate(
				paymentDecisionReliability(),
				requiredHumanApproval()
		);

		assertThat(reliability.level()).isEqualTo(RecommendationReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(reliability.scope())
				.isEqualTo(RecommendationReliabilityScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldDowngradeContradictoryDecision() {
		RecommendationReliability reliability = evaluator.evaluate(
				contradictoryDecisionReliability(),
				requiredHumanApproval()
		);

		assertThat(reliability.level()).isEqualTo(RecommendationReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(RecommendationReliabilityReason.CONTRADICTORY_DECISION);
		assertThat(reliability.scope()).isEqualTo(RecommendationReliabilityScope.LIFECYCLE);
	}

	@Test
	void shouldRequireAllConditionsForHighRecommendationReliability() {
		RecommendationReliability reliability = evaluator.evaluate(
				highDecisionReliability(),
				requiredHumanApproval()
		);

		assertThat(reliability.level()).isEqualTo(RecommendationReliabilityLevel.HIGH);
		assertThat(reliability.reason())
				.isEqualTo(RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY);
	}

	@Test
	void shouldRejectNullDecisionReliability() {
		assertThatThrownBy(() -> evaluator.evaluate(null, requiredHumanApproval()))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("decisionReliability must not be null");
	}

	private RecommendationReliabilityLevel dummyLevel() {
		return RecommendationReliabilityLevel.UNKNOWN;
	}

	private DecisionReliability highDecisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.HIGH,
				DecisionReliabilityReason.HIGH_ASSESSMENT_RELIABILITY,
				DecisionReliabilityScope.ASSESSMENT,
				highAssessmentReliability(),
				boundScenario(),
				boundRollbackVerification(false)
		);
	}

	private DecisionReliability lowDecisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.LOW,
				DecisionReliabilityReason.LOW_ASSESSMENT_RELIABILITY,
				DecisionReliabilityScope.OPERATOR_VIEW,
				lowAssessmentReliability(),
				boundScenario(),
				boundRollbackVerification(false)
		);
	}

	private DecisionReliability unreliableDecisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.UNRELIABLE,
				DecisionReliabilityReason.UNRELIABLE_ASSESSMENT,
				DecisionReliabilityScope.ASSESSMENT,
				unreliableAssessmentReliability(),
				boundScenario(),
				boundRollbackVerification(false)
		);
	}

	private DecisionReliability blockedDecisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.BLOCKED,
				DecisionReliabilityReason.BLOCKED_ASSESSMENT,
				DecisionReliabilityScope.ASSESSMENT,
				blockedAssessmentReliability(),
				boundScenario(),
				boundRollbackVerification(false)
		);
	}

	private DecisionReliability paymentDecisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.LOW,
				DecisionReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				DecisionReliabilityScope.PAYMENT_SAFETY,
				paymentAssessmentReliability(),
				boundScenario(),
				boundRollbackVerification(true)
		);
	}

	private DecisionReliability contradictoryDecisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.LOW,
				DecisionReliabilityReason.CONTRADICTORY_ASSESSMENT,
				DecisionReliabilityScope.LIFECYCLE,
				contradictoryAssessmentReliability(),
				boundScenario(),
				boundRollbackVerification(false)
		);
	}

	private DecisionReliability decisionReliabilityWithMissingRollback() {
		return new DecisionReliability(
				DecisionReliabilityLevel.BLOCKED,
				DecisionReliabilityReason.MISSING_ROLLBACK_BINDING,
				DecisionReliabilityScope.ROLLBACK_BOUNDARY,
				highAssessmentReliability(),
				boundScenario(),
				new RollbackVerificationBindingDecision(
						RollbackVerificationBindingStatus.REJECTED,
						null,
						verificationReference(true, false, false),
						RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE
				)
		);
	}

	private DecisionReliability decisionReliabilityWithMissingVerification() {
		return new DecisionReliability(
				DecisionReliabilityLevel.BLOCKED,
				DecisionReliabilityReason.MISSING_VERIFICATION_BINDING,
				DecisionReliabilityScope.VERIFICATION_BOUNDARY,
				highAssessmentReliability(),
				boundScenario(),
				new RollbackVerificationBindingDecision(
						RollbackVerificationBindingStatus.REJECTED,
						rollbackReference(false),
						null,
						RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE
				)
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

	private HumanApprovalDecision optionalHumanApproval() {
		return new HumanApprovalDecision(
				false,
				HumanApprovalScope.OPTIONAL,
				new HumanApprovalRequirement(false, false, false, false),
				List.of(HumanApprovalReason.LOW_RISK_APPROVAL_IS_OPTIONAL)
		);
	}

	private AssessmentReliability highAssessmentReliability() {
		return assessmentReliability(
				AssessmentReliabilityLevel.HIGH,
				AssessmentReliabilityReason.HIGH_EVIDENCE_RELIABILITY,
				true,
				evidenceReliability(
						EvidenceReliabilityLevel.HIGH,
						EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
						false,
						true,
						highConfidence()
				)
		);
	}

	private AssessmentReliability lowAssessmentReliability() {
		return assessmentReliability(
				AssessmentReliabilityLevel.LOW,
				AssessmentReliabilityReason.LOW_EVIDENCE_RELIABILITY,
				true,
				evidenceReliability(
						EvidenceReliabilityLevel.LOW,
						EvidenceReliabilityReason.LOW_CONFIDENCE,
						false,
						true,
						lowConfidence()
				)
		);
	}

	private AssessmentReliability unreliableAssessmentReliability() {
		return assessmentReliability(
				AssessmentReliabilityLevel.UNRELIABLE,
				AssessmentReliabilityReason.UNRELIABLE_EVIDENCE,
				false,
				evidenceReliability(
						EvidenceReliabilityLevel.UNRELIABLE,
						EvidenceReliabilityReason.UNTRUSTED_EVIDENCE,
						false,
						false,
						unreliableConfidence()
				)
		);
	}

	private AssessmentReliability blockedAssessmentReliability() {
		return assessmentReliability(
				AssessmentReliabilityLevel.BLOCKED,
				AssessmentReliabilityReason.BLOCKED_EVIDENCE,
				false,
				evidenceReliability(
						EvidenceReliabilityLevel.BLOCKED,
						EvidenceReliabilityReason.GOVERNANCE_BLOCKED,
						false,
						false,
						blockedConfidence()
				)
		);
	}

	private AssessmentReliability paymentAssessmentReliability() {
		return assessmentReliability(
				AssessmentReliabilityLevel.LOW,
				AssessmentReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				false,
				evidenceReliability(
						EvidenceReliabilityLevel.RESTRICTED,
						EvidenceReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
						true,
						false,
						paymentConfidence()
				)
		);
	}

	private AssessmentReliability contradictoryAssessmentReliability() {
		return assessmentReliability(
				AssessmentReliabilityLevel.LOW,
				AssessmentReliabilityReason.CONTRADICTORY_EVIDENCE,
				true,
				evidenceReliability(
						EvidenceReliabilityLevel.LOW,
						EvidenceReliabilityReason.CONTRADICTORY_EVIDENCE,
						false,
						true,
						contradictoryConfidence()
				)
		);
	}

	private AssessmentReliability assessmentReliability(
			AssessmentReliabilityLevel level,
			AssessmentReliabilityReason reason,
			boolean assessmentCertaintyAllowed,
			EvidenceReliability evidenceReliability
	) {
		return new AssessmentReliability(
				level,
				reason,
				evidenceReliability.scope() == EvidenceReliabilityScope.PAYMENT_EVIDENCE
						? AssessmentReliabilityScope.PAYMENT_EVIDENCE
						: AssessmentReliabilityScope.ASSESSMENT,
				evidenceReliability,
				assessmentCertaintyAllowed
		);
	}

	private EvidenceReliability evidenceReliability(
			EvidenceReliabilityLevel level,
			EvidenceReliabilityReason reason,
			boolean paymentSafetyUncertainty,
			boolean assessmentCertaintyAllowed,
			EvidenceConfidence confidence
	) {
		return new EvidenceReliability(
				level,
				reason,
				confidence.scope() == EvidenceConfidenceScope.PAYMENT_EVIDENCE
						? EvidenceReliabilityScope.PAYMENT_EVIDENCE
						: EvidenceReliabilityScope.ASSESSMENT,
				confidence.trustScore().governanceIntegrationResult().governancePolicy(),
				confidence.trustScore().lineageIntegrationResult().lineage(),
				confidence.trustScore(),
				confidence,
				assessmentCertaintyAllowed,
				paymentSafetyUncertainty
		);
	}

	private EvidenceConfidence highConfidence() {
		return confidence(
				EvidenceConfidenceLevel.HIGH,
				EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
				EvidenceConfidenceScope.ASSESSMENT,
				highTrustScore()
		);
	}

	private EvidenceConfidence lowConfidence() {
		return confidence(
				EvidenceConfidenceLevel.LOW,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
				EvidenceConfidenceScope.OPERATOR_VIEW,
				lowTrustScore()
		);
	}

	private EvidenceConfidence unreliableConfidence() {
		return confidence(
				EvidenceConfidenceLevel.MEDIUM,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
				EvidenceConfidenceScope.ASSESSMENT,
				unreliableTrustScore()
		);
	}

	private EvidenceConfidence blockedConfidence() {
		return confidence(
				EvidenceConfidenceLevel.LOW,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
				EvidenceConfidenceScope.API_BOUNDARY,
				blockedTrustScore()
		);
	}

	private EvidenceConfidence paymentConfidence() {
		return confidence(
				EvidenceConfidenceLevel.INSUFFICIENT,
				EvidenceConfidenceReason.PAYMENT_EVIDENCE_MISSING,
				EvidenceConfidenceScope.PAYMENT_EVIDENCE,
				paymentTrustScore()
		);
	}

	private EvidenceConfidence contradictoryConfidence() {
		return confidence(
				EvidenceConfidenceLevel.LOW,
				EvidenceConfidenceReason.CONTRADICTORY_EVIDENCE,
				EvidenceConfidenceScope.OPERATOR_VIEW,
				contradictoryTrustScore()
		);
	}

	private EvidenceConfidence confidence(
			EvidenceConfidenceLevel level,
			EvidenceConfidenceReason reason,
			EvidenceConfidenceScope scope,
			EvidenceTrustScore trustScore
	) {
		return new EvidenceConfidence(level, reason, scope, trustScore);
	}

	private EvidenceTrustScore highTrustScore() {
		return trustScore(
				EvidenceTrustScoreLevel.HIGH,
				EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
				lineage(
						EvidenceLineageStatus.COMPLETE,
						EvidenceLineageReason.UNKNOWN,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						)
				)
		);
	}

	private EvidenceTrustScore lowTrustScore() {
		return trustScore(
				EvidenceTrustScoreLevel.LOW,
				EvidenceTrustScoreReason.DEGRADED_INTEGRITY,
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.DEGRADED,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
				lineage(
						EvidenceLineageStatus.COMPLETE,
						EvidenceLineageReason.UNKNOWN,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.DEGRADED,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						)
				)
		);
	}

	private EvidenceTrustScore unreliableTrustScore() {
		return trustScore(
				EvidenceTrustScoreLevel.UNTRUSTED,
				EvidenceTrustScoreReason.MISSING_PROVENANCE,
				governancePolicy(
						EvidenceTrustLevel.UNTRUSTED,
						EvidenceIntegrityStatus.MISSING,
						EvidenceClassification.UNKNOWN,
						EvidenceProvenance.missingProvenance()
				),
				lineage(
						EvidenceLineageStatus.PARTIAL,
						EvidenceLineageReason.MISSING_PROVENANCE,
						governancePolicy(
								EvidenceTrustLevel.UNTRUSTED,
								EvidenceIntegrityStatus.MISSING,
								EvidenceClassification.UNKNOWN,
								EvidenceProvenance.missingProvenance()
						)
				)
		);
	}

	private EvidenceTrustScore blockedTrustScore() {
		return trustScore(
				EvidenceTrustScoreLevel.UNTRUSTED,
				EvidenceTrustScoreReason.BLOCKED_EVIDENCE,
				governancePolicy(
						EvidenceTrustLevel.UNTRUSTED,
						EvidenceIntegrityStatus.DEGRADED,
						EvidenceClassification.BLOCKED,
						provenance(true, false, true)
				),
				lineage(
						EvidenceLineageStatus.BLOCKED,
						EvidenceLineageReason.BLOCKED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.UNTRUSTED,
								EvidenceIntegrityStatus.DEGRADED,
								EvidenceClassification.BLOCKED,
								provenance(true, false, true)
						)
				)
		);
	}

	private EvidenceTrustScore paymentTrustScore() {
		return trustScore(
				EvidenceTrustScoreLevel.MEDIUM,
				EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE,
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.RESTRICTED,
						provenance(true, false, false)
				),
				lineage(
						EvidenceLineageStatus.RESTRICTED,
						EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.RESTRICTED,
								provenance(true, false, false)
						)
				)
		);
	}

	private EvidenceTrustScore contradictoryTrustScore() {
		return trustScore(
				EvidenceTrustScoreLevel.LOW,
				EvidenceTrustScoreReason.CONTRADICTORY_EVIDENCE,
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.CONTRADICTORY,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
				lineage(
						EvidenceLineageStatus.PARTIAL,
						EvidenceLineageReason.CONTRADICTORY_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.CONTRADICTORY,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						)
				)
		);
	}

	private EvidenceTrustScore trustScore(
			EvidenceTrustScoreLevel level,
			EvidenceTrustScoreReason reason,
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage
	) {
		EvidenceRuntimeApiResponse apiResponse = apiResponse();
		return new EvidenceTrustScore(
				level,
				reason,
				governancePolicy.classification() == EvidenceClassification.RESTRICTED
						? EvidenceTrustScoreScope.PAYMENT_EVIDENCE
						: EvidenceTrustScoreScope.EVIDENCE,
				new EvidenceGovernanceIntegrationResult(
						governancePolicy,
						apiResponse,
						EvidenceGovernanceIntegrationStatus.INTEGRATED,
						EvidenceGovernanceIntegrationReason.UNKNOWN,
						EvidenceGovernanceIntegrationScope.API_EXPOSURE
				),
				new EvidenceLineageIntegrationResult(
						lineage,
						apiResponse,
						EvidenceLineageIntegrationStatus.INTEGRATED,
						EvidenceLineageIntegrationReason.UNKNOWN,
						EvidenceLineageIntegrationScope.API_EXPOSURE
				)
		);
	}

	private ScenarioBindingDecision boundScenario() {
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.BOUND,
				new ScenarioReference("scenario-1", "knowledge-1", true, false),
				null
		);
	}

	private RollbackVerificationBindingDecision boundRollbackVerification(
			boolean paymentSafety
	) {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.BOUND,
				rollbackReference(false),
				verificationReference(paymentSafety, false, true),
				null
		);
	}

	private RollbackReference rollbackReference(boolean deprecated) {
		return new RollbackReference("rollback-1", "knowledge-1", true, deprecated);
	}

	private VerificationReference verificationReference(
			boolean paymentConsistencyVerification,
			boolean deprecated,
			boolean known
	) {
		return new VerificationReference(
				"verification-1",
				"knowledge-1",
				known,
				deprecated,
				paymentConsistencyVerification
		);
	}

	private EvidenceLineage lineage(
			EvidenceLineageStatus status,
			EvidenceLineageReason reason,
			EvidenceGovernancePolicy governancePolicy
	) {
		return new EvidenceLineage(
				List.of(
						EvidenceLineageNode.SOURCE,
						EvidenceLineageNode.ADAPTER,
						EvidenceLineageNode.ROUTING,
						EvidenceLineageNode.DISPATCH,
						EvidenceLineageNode.EXECUTION,
						EvidenceLineageNode.COLLECTION,
						EvidenceLineageNode.ASSESSMENT,
						EvidenceLineageNode.SUMMARY
				),
				List.of(
						new EvidenceLineageEdge(EvidenceLineageNode.SOURCE, EvidenceLineageNode.ADAPTER),
						new EvidenceLineageEdge(EvidenceLineageNode.ADAPTER, EvidenceLineageNode.ROUTING)
				),
				status,
				reason,
				governancePolicy,
				status == EvidenceLineageStatus.BLOCKED
						? OperationalUncertainty.CRITICAL
						: status == EvidenceLineageStatus.RESTRICTED
						? OperationalUncertainty.HIGH
						: OperationalUncertainty.LOW
		);
	}

	private EvidenceGovernancePolicy governancePolicy(
			EvidenceTrustLevel trustLevel,
			EvidenceIntegrityStatus integrityStatus,
			EvidenceClassification classification,
			EvidenceProvenance provenance
	) {
		return new EvidenceGovernancePolicy(
				trustLevel,
				integrityStatus,
				classification,
				provenance
		);
	}

	private EvidenceProvenance provenance(
			boolean sanitized,
			boolean rawPayloadPresent,
			boolean sensitiveDataPresent
	) {
		return new EvidenceProvenance(
				EvidenceSourceType.METRICS,
				"adapter-1",
				Instant.parse("2026-06-03T00:00:00Z"),
				sanitized,
				rawPayloadPresent,
				sensitiveDataPresent
		);
	}

	private EvidenceRuntimeApiResponse apiResponse() {
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
}
