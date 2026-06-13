package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityActionAdmissionReliabilityTest {

	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String ACTION_TYPE = "restart-deployment";
	private static final String BLAST_RADIUS_BOUNDARY = "namespace/payments-prod";

	private final ActionAdmissionReliabilityEvaluator evaluator =
			new ActionAdmissionReliabilityEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				highVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.readOnly()).isTrue();
		assertThat(reliability.actualActionCommand()).isFalse();
		assertThat(reliability.executionPermission()).isFalse();
		assertThat(reliability.actionAdmissionResult()).isFalse();
		assertThat(reliability.operatorFacingAdmissionReadiness()).isTrue();
		assertThat(reliability.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBlockActionAdmissionForBlockedVerificationReliability() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				blockedVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.BLOCKED_VERIFICATION);
	}

	@Test
	void shouldMarkUnreliableVerificationAsUnreliableActionAdmission() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				unreliableVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.UNRELIABLE);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.UNRELIABLE_VERIFICATION);
	}

	@Test
	void shouldDowngradeLowVerificationReliability() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				lowVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.LOW_VERIFICATION_RELIABILITY);
	}

	@Test
	void shouldBlockActionAdmissionWhenActionTypeMissing() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				highVerificationReliability(),
				" ",
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.MISSING_ACTION_TYPE);
		assertThat(reliability.scope()).isEqualTo(ActionAdmissionReliabilityScope.ACTION_TYPE);
	}

	@Test
	void shouldBlockActionAdmissionWhenBlastRadiusBoundaryMissing() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				highVerificationReliability(),
				ACTION_TYPE,
				" "
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.MISSING_BLAST_RADIUS_BOUNDARY);
		assertThat(reliability.scope()).isEqualTo(ActionAdmissionReliabilityScope.BLAST_RADIUS);
	}

	@Test
	void shouldBlockActionAdmissionWhenRollbackBindingMissing() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				verificationReliabilityWithMissingRollback(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.MISSING_ROLLBACK_BINDING);
		assertThat(reliability.scope())
				.isEqualTo(ActionAdmissionReliabilityScope.ROLLBACK_BOUNDARY);
	}

	@Test
	void shouldBlockActionAdmissionWhenVerificationBindingMissing() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				verificationReliabilityWithMissingVerification(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.MISSING_VERIFICATION_BINDING);
		assertThat(reliability.scope())
				.isEqualTo(ActionAdmissionReliabilityScope.VERIFICATION_BOUNDARY);
	}

	@Test
	void shouldBlockActionAdmissionWhenHumanApprovalRequirementMissing() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				verificationReliabilityWithoutHumanApprovalRequirement(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason()).isEqualTo(
				ActionAdmissionReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT
		);
		assertThat(reliability.scope()).isEqualTo(ActionAdmissionReliabilityScope.HUMAN_APPROVAL);
	}

	@Test
	void shouldDowngradeActionAdmissionForPaymentSafetyUncertainty() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				paymentVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(reliability.scope()).isEqualTo(ActionAdmissionReliabilityScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldDowngradeContradictoryVerification() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				contradictoryVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.CONTRADICTORY_VERIFICATION);
		assertThat(reliability.scope()).isEqualTo(ActionAdmissionReliabilityScope.LIFECYCLE);
	}

	@Test
	void shouldRequireAllConditionsForHighActionAdmissionReliability() {
		ActionAdmissionReliability reliability = evaluator.evaluate(
				highVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(reliability.level()).isEqualTo(ActionAdmissionReliabilityLevel.HIGH);
		assertThat(reliability.reason())
				.isEqualTo(ActionAdmissionReliabilityReason.HIGH_VERIFICATION_RELIABILITY);
	}

	@Test
	void shouldRejectNullVerificationReliability() {
		assertThatThrownBy(() -> evaluator.evaluate(null, ACTION_TYPE, BLAST_RADIUS_BOUNDARY))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("verificationReliability must not be null");
	}

	private VerificationReliability highVerificationReliability() {
		return new VerificationReliability(
				VerificationReliabilityLevel.HIGH,
				VerificationReliabilityReason.HIGH_APPROVAL_RELIABILITY,
				VerificationReliabilityScope.APPROVAL,
				highApprovalReliability(),
				true
		);
	}

	private VerificationReliability lowVerificationReliability() {
		return new VerificationReliability(
				VerificationReliabilityLevel.LOW,
				VerificationReliabilityReason.LOW_APPROVAL_RELIABILITY,
				VerificationReliabilityScope.OPERATOR_VIEW,
				lowApprovalReliability(),
				true
		);
	}

	private VerificationReliability unreliableVerificationReliability() {
		return new VerificationReliability(
				VerificationReliabilityLevel.UNRELIABLE,
				VerificationReliabilityReason.UNRELIABLE_APPROVAL,
				VerificationReliabilityScope.APPROVAL,
				unreliableApprovalReliability(),
				true
		);
	}

	private VerificationReliability blockedVerificationReliability() {
		return new VerificationReliability(
				VerificationReliabilityLevel.BLOCKED,
				VerificationReliabilityReason.BLOCKED_APPROVAL,
				VerificationReliabilityScope.APPROVAL,
				blockedApprovalReliability(),
				true
		);
	}

	private VerificationReliability paymentVerificationReliability() {
		return new VerificationReliability(
				VerificationReliabilityLevel.LOW,
				VerificationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				VerificationReliabilityScope.PAYMENT_SAFETY,
				paymentApprovalReliability(),
				true
		);
	}

	private VerificationReliability contradictoryVerificationReliability() {
		return new VerificationReliability(
				VerificationReliabilityLevel.LOW,
				VerificationReliabilityReason.CONTRADICTORY_APPROVAL,
				VerificationReliabilityScope.LIFECYCLE,
				contradictoryApprovalReliability(),
				true
		);
	}

	private VerificationReliability verificationReliabilityWithMissingRollback() {
		return new VerificationReliability(
				VerificationReliabilityLevel.BLOCKED,
				VerificationReliabilityReason.MISSING_ROLLBACK_BINDING,
				VerificationReliabilityScope.ROLLBACK_BOUNDARY,
				approvalReliabilityWithMissingRollback(),
				true
		);
	}

	private VerificationReliability verificationReliabilityWithMissingVerification() {
		return new VerificationReliability(
				VerificationReliabilityLevel.BLOCKED,
				VerificationReliabilityReason.MISSING_VERIFICATION_BINDING,
				VerificationReliabilityScope.VERIFICATION,
				approvalReliabilityWithMissingVerification(),
				true
		);
	}

	private VerificationReliability verificationReliabilityWithoutHumanApprovalRequirement() {
		return new VerificationReliability(
				VerificationReliabilityLevel.HIGH,
				VerificationReliabilityReason.HIGH_APPROVAL_RELIABILITY,
				VerificationReliabilityScope.APPROVAL,
				approvalReliabilityWithoutHumanApprovalRequirement(),
				true
		);
	}

	private ApprovalReliability highApprovalReliability() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.HIGH,
				ApprovalReliabilityReason.HIGH_RECOMMENDATION_RELIABILITY,
				ApprovalReliabilityScope.RECOMMENDATION,
				highRecommendationReliability(),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReliability lowApprovalReliability() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.LOW,
				ApprovalReliabilityReason.LOW_RECOMMENDATION_RELIABILITY,
				ApprovalReliabilityScope.OPERATOR_VIEW,
				lowRecommendationReliability(),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReliability unreliableApprovalReliability() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.UNRELIABLE,
				ApprovalReliabilityReason.UNRELIABLE_RECOMMENDATION,
				ApprovalReliabilityScope.RECOMMENDATION,
				unreliableRecommendationReliability(),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReliability blockedApprovalReliability() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.BLOCKED,
				ApprovalReliabilityReason.BLOCKED_RECOMMENDATION,
				ApprovalReliabilityScope.RECOMMENDATION,
				blockedRecommendationReliability(),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReliability paymentApprovalReliability() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.LOW,
				ApprovalReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				ApprovalReliabilityScope.PAYMENT_SAFETY,
				paymentRecommendationReliability(),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReliability contradictoryApprovalReliability() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.LOW,
				ApprovalReliabilityReason.CONTRADICTORY_RECOMMENDATION,
				ApprovalReliabilityScope.LIFECYCLE,
				contradictoryRecommendationReliability(),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReliability approvalReliabilityWithMissingRollback() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.BLOCKED,
				ApprovalReliabilityReason.MISSING_ROLLBACK_BINDING,
				ApprovalReliabilityScope.ROLLBACK_BOUNDARY,
				recommendationReliabilityWithMissingRollback(),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReliability approvalReliabilityWithMissingVerification() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.BLOCKED,
				ApprovalReliabilityReason.MISSING_VERIFICATION_BINDING,
				ApprovalReliabilityScope.VERIFICATION_BOUNDARY,
				recommendationReliabilityWithMissingVerification(),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReliability approvalReliabilityWithoutHumanApprovalRequirement() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.HIGH,
				ApprovalReliabilityReason.HIGH_RECOMMENDATION_RELIABILITY,
				ApprovalReliabilityScope.RECOMMENDATION,
				recommendationReliabilityWithoutHumanApprovalRequirement(),
				OPERATOR_CONTEXT
		);
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

	private RecommendationReliability recommendationReliabilityWithoutHumanApprovalRequirement() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY,
				RecommendationReliabilityScope.DECISION,
				highDecisionReliability(),
				new HumanApprovalDecision(
						false,
						HumanApprovalScope.OPTIONAL,
						new HumanApprovalRequirement(false, false, false, false),
						List.of()
				)
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
				false,
				evidenceReliability(
						EvidenceReliabilityLevel.LOW,
						EvidenceReliabilityReason.CONTRADICTORY_EVIDENCE,
						false,
						false,
						contradictoryConfidence()
				)
		);
	}

	private AssessmentReliability assessmentReliability(
			AssessmentReliabilityLevel level,
			AssessmentReliabilityReason reason,
			boolean assessmentCertain,
			EvidenceReliability evidenceReliability
	) {
		return new AssessmentReliability(
				level,
				reason,
				AssessmentReliabilityScope.ASSESSMENT,
				evidenceReliability,
				assessmentCertain
		);
	}

	private EvidenceReliability evidenceReliability(
			EvidenceReliabilityLevel level,
			EvidenceReliabilityReason reason,
			boolean paymentSafetyUncertainty,
			boolean trusted,
			EvidenceConfidence confidence
	) {
		EvidenceGovernancePolicy governancePolicy = governancePolicy(
				level == EvidenceReliabilityLevel.BLOCKED,
				level == EvidenceReliabilityLevel.RESTRICTED,
				reason == EvidenceReliabilityReason.CONTRADICTORY_EVIDENCE
		);
		EvidenceLineage lineage = EvidenceLineage.trace(governancePolicy, true, true);
		EvidenceTrustScore trustScore = trustScore(
				governancePolicy,
				lineage,
				trusted ? EvidenceTrustScoreLevel.HIGH : EvidenceTrustScoreLevel.LOW,
				trusted
						? EvidenceTrustScoreReason.TRUSTED_PROVENANCE
						: EvidenceTrustScoreReason.DEGRADED_INTEGRITY
		);
		return new EvidenceReliability(
				level,
				reason,
				EvidenceReliabilityScope.EVIDENCE,
				governancePolicy,
				lineage,
				trustScore,
				confidence,
				level == EvidenceReliabilityLevel.HIGH,
				paymentSafetyUncertainty
		);
	}

	private EvidenceGovernancePolicy governancePolicy(
			boolean blocked,
			boolean restricted,
			boolean contradictory
	) {
		EvidenceProvenance provenance = new EvidenceProvenance(
				EvidenceSourceType.METRICS,
				"adapter/prometheus",
				Instant.parse("2026-06-11T00:00:00Z"),
				!blocked,
				false,
				blocked
		);
		return new EvidenceGovernancePolicy(
				blocked ? EvidenceTrustLevel.UNTRUSTED : EvidenceTrustLevel.TRUSTED,
				contradictory
						? EvidenceIntegrityStatus.CONTRADICTORY
						: EvidenceIntegrityStatus.INTACT,
				blocked
						? EvidenceClassification.BLOCKED
						: restricted
						? EvidenceClassification.RESTRICTED
						: EvidenceClassification.PUBLIC_SAFE,
				provenance
		);
	}

	private EvidenceTrustScore trustScore(
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage,
			EvidenceTrustScoreLevel level,
			EvidenceTrustScoreReason reason
	) {
		return new EvidenceTrustScore(
				level,
				reason,
				EvidenceTrustScoreScope.EVIDENCE,
				governanceResult(governancePolicy),
				lineageResult(lineage)
		);
	}

	private EvidenceGovernanceIntegrationResult governanceResult(
			EvidenceGovernancePolicy governancePolicy
	) {
		return new EvidenceGovernanceIntegrationResult(
				governancePolicy,
				readableApiResponse(),
				governancePolicy.classification() == EvidenceClassification.BLOCKED
						? EvidenceGovernanceIntegrationStatus.BLOCKED
						: governancePolicy.classification() == EvidenceClassification.RESTRICTED
						? EvidenceGovernanceIntegrationStatus.RESTRICTED
						: EvidenceGovernanceIntegrationStatus.INTEGRATED,
				governancePolicy.classification() == EvidenceClassification.BLOCKED
						? EvidenceGovernanceIntegrationReason.BLOCKED_CLASSIFICATION
						: governancePolicy.classification() == EvidenceClassification.RESTRICTED
						? EvidenceGovernanceIntegrationReason.PAYMENT_RESTRICTED_CLASSIFICATION
						: EvidenceGovernanceIntegrationReason.UNKNOWN,
				governancePolicy.classification() == EvidenceClassification.BLOCKED
						? EvidenceGovernanceIntegrationScope.API_BLOCKED
						: governancePolicy.classification() == EvidenceClassification.RESTRICTED
						? EvidenceGovernanceIntegrationScope.OPERATOR_FACING_RESTRICTED
						: EvidenceGovernanceIntegrationScope.API_EXPOSURE
		);
	}

	private EvidenceLineageIntegrationResult lineageResult(EvidenceLineage lineage) {
		return new EvidenceLineageIntegrationResult(
				lineage,
				readableApiResponse(),
				lineage.status() == EvidenceLineageStatus.BLOCKED
						? EvidenceLineageIntegrationStatus.BLOCKED
						: lineage.status() == EvidenceLineageStatus.RESTRICTED
						? EvidenceLineageIntegrationStatus.RESTRICTED
						: EvidenceLineageIntegrationStatus.INTEGRATED,
				lineage.status() == EvidenceLineageStatus.BLOCKED
						? EvidenceLineageIntegrationReason.BLOCKED_LINEAGE
						: lineage.status() == EvidenceLineageStatus.RESTRICTED
						? EvidenceLineageIntegrationReason.RESTRICTED_LINEAGE
						: lineage.reason() == EvidenceLineageReason.CONTRADICTORY_EVIDENCE
						? EvidenceLineageIntegrationReason.CONTRADICTORY_LINEAGE_RISK
						: EvidenceLineageIntegrationReason.UNKNOWN,
				lineage.status() == EvidenceLineageStatus.BLOCKED
						? EvidenceLineageIntegrationScope.API_BLOCKED
						: lineage.status() == EvidenceLineageStatus.RESTRICTED
						? EvidenceLineageIntegrationScope.OPERATOR_FACING_RESTRICTED
						: EvidenceLineageIntegrationScope.API_EXPOSURE
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

	private EvidenceConfidence highConfidence() {
		EvidenceTrustScore trustScore = trustScore(
				governancePolicy(false, false, false),
				EvidenceLineage.trace(governancePolicy(false, false, false), true, true),
				EvidenceTrustScoreLevel.HIGH,
				EvidenceTrustScoreReason.TRUSTED_PROVENANCE
		);
		return new EvidenceConfidence(
				EvidenceConfidenceLevel.HIGH,
				EvidenceConfidenceReason.SUFFICIENT_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore
		);
	}

	private EvidenceConfidence lowConfidence() {
		EvidenceTrustScore trustScore = trustScore(
				governancePolicy(false, false, false),
				EvidenceLineage.trace(governancePolicy(false, false, false), true, true),
				EvidenceTrustScoreLevel.MEDIUM,
				EvidenceTrustScoreReason.DEGRADED_INTEGRITY
		);
		return new EvidenceConfidence(
				EvidenceConfidenceLevel.LOW,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore
		);
	}

	private EvidenceConfidence unreliableConfidence() {
		EvidenceTrustScore trustScore = trustScore(
				governancePolicy(false, false, false),
				EvidenceLineage.trace(governancePolicy(false, false, false), true, true),
				EvidenceTrustScoreLevel.LOW,
				EvidenceTrustScoreReason.DEGRADED_INTEGRITY
		);
		return new EvidenceConfidence(
				EvidenceConfidenceLevel.INSUFFICIENT,
				EvidenceConfidenceReason.INSUFFICIENT_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore
		);
	}

	private EvidenceConfidence blockedConfidence() {
		EvidenceTrustScore trustScore = trustScore(
				governancePolicy(true, false, false),
				EvidenceLineage.trace(governancePolicy(true, false, false), true, true),
				EvidenceTrustScoreLevel.UNTRUSTED,
				EvidenceTrustScoreReason.BLOCKED_EVIDENCE
		);
		return new EvidenceConfidence(
				EvidenceConfidenceLevel.UNKNOWN,
				EvidenceConfidenceReason.UNKNOWN,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore
		);
	}

	private EvidenceConfidence paymentConfidence() {
		EvidenceTrustScore trustScore = trustScore(
				governancePolicy(false, true, false),
				EvidenceLineage.trace(governancePolicy(false, true, false), true, true),
				EvidenceTrustScoreLevel.MEDIUM,
				EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE
		);
		return new EvidenceConfidence(
				EvidenceConfidenceLevel.LOW,
				EvidenceConfidenceReason.PAYMENT_EVIDENCE_MISSING,
				EvidenceConfidenceScope.PAYMENT_EVIDENCE,
				trustScore
		);
	}

	private EvidenceConfidence contradictoryConfidence() {
		EvidenceTrustScore trustScore = trustScore(
				governancePolicy(false, false, true),
				EvidenceLineage.trace(governancePolicy(false, false, true), true, true),
				EvidenceTrustScoreLevel.LOW,
				EvidenceTrustScoreReason.CONTRADICTORY_EVIDENCE
		);
		return new EvidenceConfidence(
				EvidenceConfidenceLevel.LOW,
				EvidenceConfidenceReason.CONTRADICTORY_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore
		);
	}

	private ScenarioBindingDecision boundScenario() {
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.BOUND,
				new ScenarioReference("payments-degradation", "scenario-knowledge", true, false),
				null
		);
	}

	private RollbackVerificationBindingDecision boundRollbackVerification(
			boolean paymentUnsafe
	) {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.BOUND,
				rollbackReference(paymentUnsafe),
				verificationReference(paymentUnsafe, true),
				null
		);
	}

	private RollbackReference rollbackReference(boolean paymentUnsafe) {
		return new RollbackReference(
				"rollback/payments",
				"rollback-knowledge",
				!paymentUnsafe,
				false
		);
	}

	private VerificationReference verificationReference(
			boolean paymentUnsafe,
			boolean postExecutionRequired
	) {
		return new VerificationReference(
				"verification/payments",
				"verification-knowledge",
				postExecutionRequired,
				false,
				paymentUnsafe
		);
	}
}
