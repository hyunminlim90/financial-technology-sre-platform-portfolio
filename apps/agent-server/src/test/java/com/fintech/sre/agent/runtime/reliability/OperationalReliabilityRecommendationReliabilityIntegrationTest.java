package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRecommendationReliabilityIntegrationTest {

	private final RecommendationReliabilityIntegration integration =
			new RecommendationReliabilityIntegration();

	@Test
	void shouldRemainReadOnlyAndNonMutating() {
		assertThat(integration.readOnly()).isTrue();
		assertThat(integration.mutatesRecommendation()).isFalse();
		assertThat(integration.recommendationAuthority()).isFalse();
		assertThat(integration.executionAuthority()).isFalse();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldForbidOperatorFacingRecommendationForBlockedRecommendationReliability() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				blockedRecommendationReliability(
						RecommendationReliabilityReason.BLOCKED_DECISION
				)
		);

		assertThat(result.status()).isEqualTo(RecommendationReliabilityIntegrationStatus.BLOCKED);
		assertThat(result.operatorFacingRecommendationAllowed()).isFalse();
		assertThat(result.scope())
				.isEqualTo(RecommendationReliabilityIntegrationScope.RECOMMENDATION_FORBIDDEN);
	}

	@Test
	void shouldPreventRecommendationCertaintyForUnreliableRecommendationReliability() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				unreliableRecommendationReliability()
		);

		assertThat(result.status())
				.isEqualTo(RecommendationReliabilityIntegrationStatus.UNRELIABLE);
		assertThat(result.recommendationCertaintyAllowed()).isFalse();
	}

	@Test
	void shouldRequireOperatorFacingWarningForLowRecommendationReliability() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				lowRecommendationReliability()
		);

		assertThat(result.status()).isEqualTo(RecommendationReliabilityIntegrationStatus.WARNING);
		assertThat(result.reason()).isEqualTo(
				RecommendationReliabilityIntegrationReason.LOW_RECOMMENDATION_RELIABILITY
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationReliabilityIntegrationScope.OPERATOR_WARNING_VIEW
		);
	}

	@Test
	void shouldMarkMediumRecommendationReliabilityAsPartial() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				mediumRecommendationReliability()
		);

		assertThat(result.status()).isEqualTo(
				RecommendationReliabilityIntegrationStatus
						.PARTIAL_RECOMMENDATION_RELIABILITY
		);
		assertThat(result.reason()).isEqualTo(
				RecommendationReliabilityIntegrationReason
						.MEDIUM_RECOMMENDATION_RELIABILITY
		);
	}

	@Test
	void shouldAllowOnlyHighRecommendationReliabilityAsReliableRecommendationViewCandidate() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				highRecommendationReliability()
		);

		assertThat(result.status()).isEqualTo(RecommendationReliabilityIntegrationStatus.RELIABLE);
		assertThat(result.reason()).isEqualTo(
				RecommendationReliabilityIntegrationReason
						.HIGH_RECOMMENDATION_RELIABILITY
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationReliabilityIntegrationScope.RELIABLE_RECOMMENDATION_VIEW
		);
		assertThat(result.operatorFacingRecommendationAllowed()).isTrue();
		assertThat(result.recommendationCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldPropagateMissingHumanApprovalRequirementToLifecycleUncertainty() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				blockedRecommendationReliability(
						RecommendationReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT
				)
		);

		assertThat(result.reason()).isEqualTo(
				RecommendationReliabilityIntegrationReason
						.MISSING_HUMAN_APPROVAL_REQUIREMENT
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationReliabilityIntegrationScope.HUMAN_APPROVAL_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldPropagateMissingRollbackBindingToLifecycleUncertainty() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				blockedRecommendationReliability(
						RecommendationReliabilityReason.MISSING_ROLLBACK_BINDING
				)
		);

		assertThat(result.reason()).isEqualTo(
				RecommendationReliabilityIntegrationReason.MISSING_ROLLBACK_BINDING
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationReliabilityIntegrationScope.ROLLBACK_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldPropagateMissingVerificationBindingToLifecycleUncertainty() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				blockedRecommendationReliability(
						RecommendationReliabilityReason.MISSING_VERIFICATION_BINDING
				)
		);

		assertThat(result.reason()).isEqualTo(
				RecommendationReliabilityIntegrationReason.MISSING_VERIFICATION_BINDING
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationReliabilityIntegrationScope.VERIFICATION_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldKeepPaymentSafetyUncertaintyAsCriticalLifecycleRisk() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				paymentRecommendationReliability()
		);

		assertThat(result.reason()).isEqualTo(
				RecommendationReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationReliabilityIntegrationScope.PAYMENT_CRITICAL_RISK_VIEW
		);
		assertThat(result.apiResponse().summary().riskLevel())
				.isEqualTo(OperationalUncertainty.CRITICAL);
		assertThat(result.apiResponse().summary().paymentSafetyState())
				.isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldPropagateContradictoryRecommendationToLifecycleUncertainty() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				contradictoryRecommendationReliability()
		);

		assertThat(result.reason()).isEqualTo(
				RecommendationReliabilityIntegrationReason.CONTRADICTORY_RECOMMENDATION
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationReliabilityIntegrationScope.LIFECYCLE_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionAuthority() {
		RecommendationReliabilityIntegrationResult result = integration.integrate(
				highRecommendationReliability()
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.actionAdmission()).isFalse();
		assertThat(result.humanApproval()).isFalse();
	}

	@Test
	void shouldRejectNullRecommendationReliability() {
		assertThatThrownBy(() -> integration.integrate(null))
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

	private RecommendationReliability mediumRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.MEDIUM,
				RecommendationReliabilityReason.UNKNOWN,
				RecommendationReliabilityScope.OPERATOR_VIEW,
				mediumDecisionReliability(),
				explicitHumanApproval()
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

	private RecommendationReliability blockedRecommendationReliability(
			RecommendationReliabilityReason reason
	) {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.BLOCKED,
				reason,
				reason == RecommendationReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT
						? RecommendationReliabilityScope.HUMAN_APPROVAL
						: reason == RecommendationReliabilityReason.MISSING_ROLLBACK_BINDING
						? RecommendationReliabilityScope.ROLLBACK_BOUNDARY
						: reason == RecommendationReliabilityReason
								.MISSING_VERIFICATION_BINDING
						? RecommendationReliabilityScope.VERIFICATION_BOUNDARY
						: RecommendationReliabilityScope.DECISION,
				reason == RecommendationReliabilityReason.MISSING_ROLLBACK_BINDING
						? decisionReliabilityWithMissingRollback()
						: reason == RecommendationReliabilityReason
								.MISSING_VERIFICATION_BINDING
						? decisionReliabilityWithMissingVerification()
						: blockedDecisionReliability(),
				reason == RecommendationReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT
						? optionalHumanApproval()
						: requiredHumanApproval()
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

	private DecisionReliability mediumDecisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.MEDIUM,
				DecisionReliabilityReason.UNKNOWN,
				DecisionReliabilityScope.OPERATOR_VIEW,
				mediumAssessmentReliability(),
				restrictedScenario(),
				restrictedRollbackVerification()
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
						RollbackVerificationBindingRejectionReason
								.MISSING_VERIFICATION_REFERENCE
				)
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

	private HumanApprovalDecision explicitHumanApproval() {
		return new HumanApprovalDecision(
				true,
				HumanApprovalScope.CRITICAL_EXPLICIT,
				new HumanApprovalRequirement(true, true, true, false),
				List.of(HumanApprovalReason.CRITICAL_RISK_REQUIRES_EXPLICIT_APPROVAL)
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

	private AssessmentReliability mediumAssessmentReliability() {
		return assessmentReliability(
				AssessmentReliabilityLevel.MEDIUM,
				AssessmentReliabilityReason.UNKNOWN,
				false,
				evidenceReliability(
						EvidenceReliabilityLevel.MEDIUM,
						EvidenceReliabilityReason.INCOMPLETE_LINEAGE,
						false,
						false,
						mediumConfidence()
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

	private EvidenceConfidence mediumConfidence() {
		return confidence(
				EvidenceConfidenceLevel.MEDIUM,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
				EvidenceConfidenceScope.OPERATOR_VIEW,
				mediumTrustScore()
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

	private EvidenceTrustScore mediumTrustScore() {
		return trustScore(
				EvidenceTrustScoreLevel.MEDIUM,
				EvidenceTrustScoreReason.PARTIAL_PROVENANCE,
				governancePolicy(
						EvidenceTrustLevel.PARTIALLY_TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.INTERNAL,
						provenance(true, false, false)
				),
				lineage(
						EvidenceLineageStatus.RESTRICTED,
						EvidenceLineageReason.GOVERNANCE_PROTECTED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.PARTIALLY_TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.INTERNAL,
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
		EvidenceRuntimeApiBoundary apiBoundary = new EvidenceRuntimeApiBoundary();
		EvidenceRuntimeApiRequest apiRequest = new EvidenceRuntimeApiRequest(
				new EvidenceRuntimeSummaryResource(),
				summary(governancePolicy, lineage)
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
		return new EvidenceTrustScore(level, reason, EvidenceTrustScoreScope.EVIDENCE,
				governanceIntegrationResult, lineageIntegrationResult);
	}

	private EvidenceRuntimeSummary summary(
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage
	) {
		boolean uncertaintyDetected =
				governancePolicy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY
						|| lineage.status() == EvidenceLineageStatus.INCOMPLETE
						|| lineage.status() == EvidenceLineageStatus.RESTRICTED
						|| lineage.status() == EvidenceLineageStatus.BLOCKED;
		EvidenceRuntimeSummaryReason uncertaintyReason =
				governancePolicy.classification() == EvidenceClassification.RESTRICTED
						? EvidenceRuntimeSummaryReason.PAYMENT_SAFETY_UNCERTAINTY
						: governancePolicy.integrityStatus()
								== EvidenceIntegrityStatus.CONTRADICTORY
						? EvidenceRuntimeSummaryReason.CONTRADICTORY_EVIDENCE
						: lineage.status() == EvidenceLineageStatus.INCOMPLETE
						? EvidenceRuntimeSummaryReason.PARTIAL_EVIDENCE
						: EvidenceRuntimeSummaryReason.UNKNOWN;
		return new EvidenceRuntimeSummary(
				summaryStatus(governancePolicy, lineage),
				lineage.riskLevel(),
				governancePolicy.classification() == EvidenceClassification.RESTRICTED
						? OperationalUncertainty.CRITICAL
						: lineage.riskLevel(),
				uncertaintyDetected,
				uncertaintyReason,
				!governancePolicy.provenance().provenanceMissing(),
				evidenceCompleteness(lineage)
		);
	}

	private EvidenceRuntimeSummaryStatus summaryStatus(
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage
	) {
		if (governancePolicy.classification() == EvidenceClassification.BLOCKED) {
			return EvidenceRuntimeSummaryStatus.UNKNOWN;
		}
		if (governancePolicy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY
				|| lineage.status() == EvidenceLineageStatus.RESTRICTED) {
			return EvidenceRuntimeSummaryStatus.UNCERTAIN;
		}
		if (lineage.status() == EvidenceLineageStatus.INCOMPLETE) {
			return EvidenceRuntimeSummaryStatus.PARTIAL;
		}
		return EvidenceRuntimeSummaryStatus.HEALTHY;
	}

	private EvidenceCompleteness evidenceCompleteness(
			EvidenceLineage lineage
	) {
		return switch (lineage.status()) {
			case COMPLETE -> EvidenceCompleteness.COMPLETE;
			case PARTIAL, RESTRICTED -> EvidenceCompleteness.PARTIAL;
			case INCOMPLETE -> EvidenceCompleteness.PARTIAL;
			case BLOCKED, UNKNOWN -> EvidenceCompleteness.ABSENT;
		};
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
						new EvidenceLineageEdge(
								EvidenceLineageNode.SOURCE,
								EvidenceLineageNode.ADAPTER
						),
						new EvidenceLineageEdge(
								EvidenceLineageNode.ADAPTER,
								EvidenceLineageNode.ROUTING
						),
						new EvidenceLineageEdge(
								EvidenceLineageNode.ROUTING,
								EvidenceLineageNode.DISPATCH
						),
						new EvidenceLineageEdge(
								EvidenceLineageNode.DISPATCH,
								EvidenceLineageNode.EXECUTION
						),
						new EvidenceLineageEdge(
								EvidenceLineageNode.EXECUTION,
								EvidenceLineageNode.SUMMARY
						)
				),
				status,
				reason,
				governancePolicy
				,
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

	private ScenarioBindingDecision restrictedScenario() {
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.RESTRICTED,
				new ScenarioReference(
						"payments/deprecated",
						"portfolio/scenario",
						true,
						true
				),
				ScenarioBindingRejectionReason.DEPRECATED_SCENARIO_HIGH_RISK_RESTRICTION
		);
	}

	private RollbackVerificationBindingDecision boundRollbackVerification(
			boolean paymentConsistencyRequired
	) {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.BOUND,
				rollbackReference(paymentConsistencyRequired),
				verificationReference(true, paymentConsistencyRequired, false),
				null
		);
	}

	private RollbackVerificationBindingDecision restrictedRollbackVerification() {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.RESTRICTED,
				new RollbackReference("rollback/legacy", "portfolio/rollback", true, true),
				new VerificationReference(
						"verification/legacy",
						"portfolio/verification",
						true,
						true,
						false
				),
				RollbackVerificationBindingRejectionReason
						.DEPRECATED_ROLLBACK_HIGH_RISK_RESTRICTION
		);
	}

	private RollbackReference rollbackReference(boolean paymentSafetyAction) {
		return new RollbackReference(
				paymentSafetyAction ? "rollback/payments-safe" : "rollback/general-safe",
				"portfolio/rollback",
				true,
				false
		);
	}

	private VerificationReference verificationReference(
			boolean paymentConsistencyVerification,
			boolean paymentSafetyAction,
			boolean deprecated
	) {
		return deprecated
				? new VerificationReference(
						"verification/payment-consistency",
						"portfolio/verification",
						true,
						true,
						paymentConsistencyVerification
				)
					: new VerificationReference(
							paymentSafetyAction
									? "verification/payment-consistency"
									: "verification/general-health",
							"portfolio/verification",
							true,
							false,
							paymentConsistencyVerification
					);
		}
}
