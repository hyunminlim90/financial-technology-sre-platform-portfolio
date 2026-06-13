package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityApprovalReliabilityTest {

	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";

	private final ApprovalReliabilityEvaluator evaluator =
			new ApprovalReliabilityEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ApprovalReliability reliability = evaluator.evaluate(
				highRecommendationReliability(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.readOnly()).isTrue();
		assertThat(reliability.actualApproval()).isFalse();
		assertThat(reliability.approvalRequest()).isFalse();
		assertThat(reliability.executionPermission()).isFalse();
		assertThat(reliability.actionAdmission()).isFalse();
		assertThat(reliability.operatorFacingApprovalReadiness()).isTrue();
		assertThat(reliability.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBlockApprovalForBlockedRecommendationReliability() {
		ApprovalReliability reliability = evaluator.evaluate(
				blockedRecommendationReliability(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.BLOCKED);
		assertThat(reliability.reason()).isEqualTo(ApprovalReliabilityReason.BLOCKED_RECOMMENDATION);
	}

	@Test
	void shouldMarkUnreliableRecommendationAsUnreliableApproval() {
		ApprovalReliability reliability = evaluator.evaluate(
				unreliableRecommendationReliability(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.UNRELIABLE);
		assertThat(reliability.reason())
				.isEqualTo(ApprovalReliabilityReason.UNRELIABLE_RECOMMENDATION);
	}

	@Test
	void shouldDowngradeLowRecommendationReliability() {
		ApprovalReliability reliability = evaluator.evaluate(
				lowRecommendationReliability(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(ApprovalReliabilityReason.LOW_RECOMMENDATION_RELIABILITY);
	}

	@Test
	void shouldBlockApprovalWhenHumanApprovalRequirementMissing() {
		ApprovalReliability reliability = evaluator.evaluate(
				recommendationReliabilityWithOptionalApproval(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(ApprovalReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT);
		assertThat(reliability.scope()).isEqualTo(ApprovalReliabilityScope.HUMAN_APPROVAL);
	}

	@Test
	void shouldBlockApprovalWhenOperatorContextMissing() {
		ApprovalReliability reliability = evaluator.evaluate(
				highRecommendationReliability(),
				"   "
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(ApprovalReliabilityReason.MISSING_OPERATOR_CONTEXT);
		assertThat(reliability.scope()).isEqualTo(ApprovalReliabilityScope.OPERATOR_CONTEXT);
	}

	@Test
	void shouldBlockApprovalWhenRollbackBindingMissing() {
		ApprovalReliability reliability = evaluator.evaluate(
				recommendationReliabilityWithMissingRollback(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(ApprovalReliabilityReason.MISSING_ROLLBACK_BINDING);
		assertThat(reliability.scope()).isEqualTo(ApprovalReliabilityScope.ROLLBACK_BOUNDARY);
	}

	@Test
	void shouldBlockApprovalWhenVerificationBindingMissing() {
		ApprovalReliability reliability = evaluator.evaluate(
				recommendationReliabilityWithMissingVerification(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(ApprovalReliabilityReason.MISSING_VERIFICATION_BINDING);
		assertThat(reliability.scope()).isEqualTo(ApprovalReliabilityScope.VERIFICATION_BOUNDARY);
	}

	@Test
	void shouldDowngradeApprovalForPaymentSafetyUncertainty() {
		ApprovalReliability reliability = evaluator.evaluate(
				paymentRecommendationReliability(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(ApprovalReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(reliability.scope()).isEqualTo(ApprovalReliabilityScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldDowngradeContradictoryRecommendation() {
		ApprovalReliability reliability = evaluator.evaluate(
				contradictoryRecommendationReliability(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(ApprovalReliabilityReason.CONTRADICTORY_RECOMMENDATION);
		assertThat(reliability.scope()).isEqualTo(ApprovalReliabilityScope.LIFECYCLE);
	}

	@Test
	void shouldRequireAllConditionsForHighApprovalReliability() {
		ApprovalReliability reliability = evaluator.evaluate(
				highRecommendationReliability(),
				OPERATOR_CONTEXT
		);

		assertThat(reliability.level()).isEqualTo(ApprovalReliabilityLevel.HIGH);
		assertThat(reliability.reason())
				.isEqualTo(ApprovalReliabilityReason.HIGH_RECOMMENDATION_RELIABILITY);
	}

	@Test
	void shouldRejectNullRecommendationReliability() {
		assertThatThrownBy(() -> evaluator.evaluate(null, OPERATOR_CONTEXT))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("recommendationReliability must not be null");
	}

	private RecommendationReliability highRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY,
				RecommendationReliabilityScope.DECISION,
				highDecisionReliability(),
				requiredHumanApproval()
		);
	}

	private RecommendationReliability lowRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.LOW,
				RecommendationReliabilityReason.LOW_DECISION_RELIABILITY,
				RecommendationReliabilityScope.OPERATOR_VIEW,
				lowDecisionReliability(),
				requiredHumanApproval()
		);
	}

	private RecommendationReliability unreliableRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.UNRELIABLE,
				RecommendationReliabilityReason.UNRELIABLE_DECISION,
				RecommendationReliabilityScope.DECISION,
				unreliableDecisionReliability(),
				requiredHumanApproval()
		);
	}

	private RecommendationReliability blockedRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.BLOCKED,
				RecommendationReliabilityReason.BLOCKED_DECISION,
				RecommendationReliabilityScope.DECISION,
				blockedDecisionReliability(),
				requiredHumanApproval()
		);
	}

	private RecommendationReliability recommendationReliabilityWithOptionalApproval() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.BLOCKED,
				RecommendationReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT,
				RecommendationReliabilityScope.HUMAN_APPROVAL,
				highDecisionReliability(),
				optionalHumanApproval()
		);
	}

	private RecommendationReliability recommendationReliabilityWithMissingRollback() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.BLOCKED,
				RecommendationReliabilityReason.MISSING_ROLLBACK_BINDING,
				RecommendationReliabilityScope.ROLLBACK_BOUNDARY,
				decisionReliabilityWithMissingRollback(),
				requiredHumanApproval()
		);
	}

	private RecommendationReliability recommendationReliabilityWithMissingVerification() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.BLOCKED,
				RecommendationReliabilityReason.MISSING_VERIFICATION_BINDING,
				RecommendationReliabilityScope.VERIFICATION_BOUNDARY,
				decisionReliabilityWithMissingVerification(),
				requiredHumanApproval()
		);
	}

	private RecommendationReliability paymentRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.LOW,
				RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				RecommendationReliabilityScope.PAYMENT_SAFETY,
				paymentDecisionReliability(),
				requiredHumanApproval()
		);
	}

	private RecommendationReliability contradictoryRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.LOW,
				RecommendationReliabilityReason.CONTRADICTORY_DECISION,
				RecommendationReliabilityScope.LIFECYCLE,
				contradictoryDecisionReliability(),
				requiredHumanApproval()
		);
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
						verificationReference(true, false),
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
						EvidenceLineageStatus.INCOMPLETE,
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
						EvidenceIntegrityStatus.MISSING,
						EvidenceClassification.BLOCKED,
						provenance(false, true, true)
				),
				lineage(
						EvidenceLineageStatus.BLOCKED,
						EvidenceLineageReason.BLOCKED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.UNTRUSTED,
								EvidenceIntegrityStatus.MISSING,
								EvidenceClassification.BLOCKED,
								provenance(false, true, true)
						)
				)
		);
	}

	private EvidenceTrustScore paymentTrustScore() {
		return trustScore(
				EvidenceTrustScoreLevel.MEDIUM,
				EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE,
				governancePolicy(
						EvidenceTrustLevel.PARTIALLY_TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.RESTRICTED,
						provenance(true, false, false)
				),
				lineage(
						EvidenceLineageStatus.RESTRICTED,
						EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.PARTIALLY_TRUSTED,
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
						EvidenceTrustLevel.PARTIALLY_TRUSTED,
						EvidenceIntegrityStatus.CONTRADICTORY,
						EvidenceClassification.INTERNAL,
						provenance(true, false, false)
				),
				lineage(
						EvidenceLineageStatus.PARTIAL,
						EvidenceLineageReason.CONTRADICTORY_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.PARTIALLY_TRUSTED,
								EvidenceIntegrityStatus.CONTRADICTORY,
								EvidenceClassification.INTERNAL,
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
		EvidenceRuntimeSummary summary = new EvidenceRuntimeSummary(
				EvidenceRuntimeSummaryStatus.HEALTHY,
				lineage.riskLevel(),
				governancePolicy.classification() == EvidenceClassification.RESTRICTED
						? OperationalUncertainty.CRITICAL
						: lineage.riskLevel(),
				false,
				EvidenceRuntimeSummaryReason.UNKNOWN,
				!governancePolicy.provenance().provenanceMissing(),
				EvidenceCompleteness.COMPLETE
		);
		EvidenceRuntimeApiBoundary apiBoundary = new EvidenceRuntimeApiBoundary();
		EvidenceRuntimeApiRequest apiRequest = new EvidenceRuntimeApiRequest(
				new EvidenceRuntimeSummaryResource(),
				summary
		);
		EvidenceGovernanceIntegrationResult governanceIntegrationResult =
				new EvidenceGovernanceIntegration().integrate(
						apiBoundary,
						apiRequest,
						governancePolicy
				);
		EvidenceLineageIntegrationResult lineageIntegrationResult =
				new EvidenceLineageIntegration().integrate(
						apiBoundary,
						apiRequest,
						lineage
				);
		return new EvidenceTrustScore(
				level,
				reason,
				EvidenceTrustScoreScope.EVIDENCE,
				governanceIntegrationResult,
				lineageIntegrationResult
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
				Instant.parse("2026-01-01T00:00:00Z"),
				sanitized,
				rawPayloadPresent,
				sensitiveDataPresent
		);
	}

	private EvidenceLineage lineage(
			EvidenceLineageStatus status,
			EvidenceLineageReason reason,
			EvidenceGovernancePolicy governancePolicy
	) {
		if (status == EvidenceLineageStatus.COMPLETE && reason == EvidenceLineageReason.UNKNOWN) {
			return EvidenceLineage.trace(governancePolicy, true, true);
		}
		return new EvidenceLineage(
				List.of(
						EvidenceLineageNode.SOURCE,
						EvidenceLineageNode.ADAPTER,
						EvidenceLineageNode.ROUTING,
						EvidenceLineageNode.DISPATCH,
						EvidenceLineageNode.EXECUTION,
						EvidenceLineageNode.SUMMARY
				),
				List.of(
						new EvidenceLineageEdge(EvidenceLineageNode.SOURCE, EvidenceLineageNode.ADAPTER),
						new EvidenceLineageEdge(EvidenceLineageNode.ADAPTER, EvidenceLineageNode.ROUTING),
						new EvidenceLineageEdge(EvidenceLineageNode.ROUTING, EvidenceLineageNode.DISPATCH),
						new EvidenceLineageEdge(EvidenceLineageNode.DISPATCH, EvidenceLineageNode.EXECUTION),
						new EvidenceLineageEdge(EvidenceLineageNode.EXECUTION, EvidenceLineageNode.SUMMARY)
				),
				status,
				reason,
				governancePolicy,
				status == EvidenceLineageStatus.BLOCKED
						? OperationalUncertainty.CRITICAL
						: status == EvidenceLineageStatus.RESTRICTED
						|| reason == EvidenceLineageReason.CONTRADICTORY_EVIDENCE
						? OperationalUncertainty.HIGH
						: status == EvidenceLineageStatus.INCOMPLETE
						? OperationalUncertainty.MODERATE
						: OperationalUncertainty.LOW
		);
	}

	private ScenarioBindingDecision boundScenario() {
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.BOUND,
				new ScenarioReference("payments/latency", "portfolio/scenario", true, false),
				null
		);
	}

	private RollbackVerificationBindingDecision boundRollbackVerification(
			boolean paymentConsistencyRequired
	) {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.BOUND,
				rollbackReference(false),
				verificationReference(paymentConsistencyRequired, false),
				null
		);
	}

	private RollbackReference rollbackReference(boolean deprecated) {
		return new RollbackReference(
				deprecated ? "rollback/legacy" : "rollback/general-safe",
				"portfolio/rollback",
				true,
				deprecated
		);
	}

	private VerificationReference verificationReference(
			boolean paymentConsistencyVerification,
			boolean deprecated
	) {
		return new VerificationReference(
				paymentConsistencyVerification
						? "verification/payment-consistency"
						: "verification/general-health",
				"portfolio/verification",
				true,
				deprecated,
				paymentConsistencyVerification
		);
	}
}
