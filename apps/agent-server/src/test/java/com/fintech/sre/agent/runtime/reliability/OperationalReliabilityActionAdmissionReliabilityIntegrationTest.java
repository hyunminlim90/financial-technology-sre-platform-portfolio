package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityActionAdmissionReliabilityIntegrationTest {

	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String ACTION_TYPE = "restart-deployment";
	private static final String BLAST_RADIUS_BOUNDARY = "namespace/payments-prod";

	private final ActionAdmissionReliabilityIntegration integration =
			new ActionAdmissionReliabilityIntegration();

	@Test
	void shouldRemainReadOnlyAndNonMutating() {
		assertThat(integration.readOnly()).isTrue();
		assertThat(integration.mutatesActionAdmission()).isFalse();
		assertThat(integration.actionAuthority()).isFalse();
		assertThat(integration.executionAuthority()).isFalse();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldHideActionCommandCandidateForBlockedActionAdmissionReliability() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				blockedActionAdmissionReliability(
						ActionAdmissionReliabilityReason.BLOCKED_VERIFICATION
				)
		);

		assertThat(result.status()).isEqualTo(ActionAdmissionReliabilityIntegrationStatus.BLOCKED);
		assertThat(result.actionCommandCandidateVisible()).isFalse();
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.ACTION_COMMAND_CANDIDATE_FORBIDDEN
		);
	}

	@Test
	void shouldPreventActionAdmissionCertaintyForUnreliableActionAdmissionReliability() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				unreliableActionAdmissionReliability()
		);

		assertThat(result.status()).isEqualTo(
				ActionAdmissionReliabilityIntegrationStatus.UNRELIABLE
		);
		assertThat(result.actionAdmissionCertaintyAllowed()).isFalse();
	}

	@Test
	void shouldRequireOperatorFacingWarningForLowActionAdmissionReliability() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				lowActionAdmissionReliability()
		);

		assertThat(result.status()).isEqualTo(ActionAdmissionReliabilityIntegrationStatus.WARNING);
		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason
						.LOW_ACTION_ADMISSION_RELIABILITY
		);
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.OPERATOR_WARNING_VIEW
		);
	}

	@Test
	void shouldMarkMediumActionAdmissionReliabilityAsPartialReadiness() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				mediumActionAdmissionReliability()
		);

		assertThat(result.status()).isEqualTo(
				ActionAdmissionReliabilityIntegrationStatus.PARTIAL_ADMISSION_READINESS
		);
		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason
						.MEDIUM_ACTION_ADMISSION_RELIABILITY
		);
	}

	@Test
	void shouldAllowOnlyHighActionAdmissionReliabilityAsAdmissionReadyViewCandidate() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				highActionAdmissionReliability()
		);

		assertThat(result.status()).isEqualTo(
				ActionAdmissionReliabilityIntegrationStatus.ACTION_ADMISSION_READY
		);
		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason
						.HIGH_ACTION_ADMISSION_RELIABILITY
		);
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.ACTION_ADMISSION_READY_VIEW
		);
		assertThat(result.actionCommandCandidateVisible()).isTrue();
		assertThat(result.actionAdmissionCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldPropagateMissingActionTypeToLifecycleUncertainty() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				blockedActionAdmissionReliability(
						ActionAdmissionReliabilityReason.MISSING_ACTION_TYPE
				)
		);

		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason.MISSING_ACTION_TYPE
		);
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.ACTION_TYPE_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldPropagateMissingBlastRadiusBoundaryToLifecycleUncertainty() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				blockedActionAdmissionReliability(
						ActionAdmissionReliabilityReason.MISSING_BLAST_RADIUS_BOUNDARY
				)
		);

		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason.MISSING_BLAST_RADIUS_BOUNDARY
		);
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.BLAST_RADIUS_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldPropagateMissingRollbackBindingToLifecycleUncertainty() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				blockedActionAdmissionReliability(
						ActionAdmissionReliabilityReason.MISSING_ROLLBACK_BINDING
				)
		);

		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason.MISSING_ROLLBACK_BINDING
		);
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.ROLLBACK_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldPropagateMissingVerificationBindingToLifecycleUncertainty() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				blockedActionAdmissionReliability(
						ActionAdmissionReliabilityReason.MISSING_VERIFICATION_BINDING
				)
		);

		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason.MISSING_VERIFICATION_BINDING
		);
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.VERIFICATION_BINDING_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldPropagateMissingHumanApprovalRequirementToLifecycleUncertainty() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				blockedActionAdmissionReliability(
						ActionAdmissionReliabilityReason
								.MISSING_HUMAN_APPROVAL_REQUIREMENT
				)
		);

		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason
						.MISSING_HUMAN_APPROVAL_REQUIREMENT
		);
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.HUMAN_APPROVAL_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldKeepPaymentSafetyUncertaintyAsCriticalLifecycleRisk() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				paymentActionAdmissionReliability()
		);

		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.PAYMENT_CRITICAL_RISK_VIEW
		);
		assertThat(result.apiResponse().summary().riskLevel())
				.isEqualTo(OperationalUncertainty.CRITICAL);
		assertThat(result.apiResponse().summary().paymentSafetyState())
				.isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldPropagateContradictoryActionAdmissionToLifecycleUncertainty() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				contradictoryActionAdmissionReliability()
		);

		assertThat(result.reason()).isEqualTo(
				ActionAdmissionReliabilityIntegrationReason.CONTRADICTORY_ACTION_ADMISSION
		);
		assertThat(result.scope()).isEqualTo(
				ActionAdmissionReliabilityIntegrationScope.LIFECYCLE_UNCERTAINTY
		);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldRemainNonActionCommandAndNonExecutionAuthority() {
		ActionAdmissionReliabilityIntegrationResult result = integration.integrate(
				highActionAdmissionReliability()
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.actualActionCommand()).isFalse();
		assertThat(result.actionAdmissionResult()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.approval()).isFalse();
	}

	@Test
	void shouldRejectNullActionAdmissionReliability() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("actionAdmissionReliability must not be null");
	}

	private ActionAdmissionReliability highActionAdmissionReliability() {
		return new ActionAdmissionReliability(
				ActionAdmissionReliabilityLevel.HIGH,
				ActionAdmissionReliabilityReason.HIGH_VERIFICATION_RELIABILITY,
				ActionAdmissionReliabilityScope.VERIFICATION,
				highVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReliability mediumActionAdmissionReliability() {
		return new ActionAdmissionReliability(
				ActionAdmissionReliabilityLevel.MEDIUM,
				ActionAdmissionReliabilityReason.UNKNOWN,
				ActionAdmissionReliabilityScope.OPERATOR_VIEW,
				mediumVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReliability lowActionAdmissionReliability() {
		return new ActionAdmissionReliability(
				ActionAdmissionReliabilityLevel.LOW,
				ActionAdmissionReliabilityReason.LOW_VERIFICATION_RELIABILITY,
				ActionAdmissionReliabilityScope.OPERATOR_VIEW,
				lowVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReliability unreliableActionAdmissionReliability() {
		return new ActionAdmissionReliability(
				ActionAdmissionReliabilityLevel.UNRELIABLE,
				ActionAdmissionReliabilityReason.UNRELIABLE_VERIFICATION,
				ActionAdmissionReliabilityScope.VERIFICATION,
				unreliableVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReliability paymentActionAdmissionReliability() {
		return new ActionAdmissionReliability(
				ActionAdmissionReliabilityLevel.LOW,
				ActionAdmissionReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				ActionAdmissionReliabilityScope.PAYMENT_SAFETY,
				paymentVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReliability contradictoryActionAdmissionReliability() {
		return new ActionAdmissionReliability(
				ActionAdmissionReliabilityLevel.LOW,
				ActionAdmissionReliabilityReason.CONTRADICTORY_VERIFICATION,
				ActionAdmissionReliabilityScope.LIFECYCLE,
				contradictoryVerificationReliability(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReliability blockedActionAdmissionReliability(
			ActionAdmissionReliabilityReason reason
	) {
		return new ActionAdmissionReliability(
				ActionAdmissionReliabilityLevel.BLOCKED,
				reason,
				reason == ActionAdmissionReliabilityReason.MISSING_ACTION_TYPE
						? ActionAdmissionReliabilityScope.ACTION_TYPE
						: reason == ActionAdmissionReliabilityReason
								.MISSING_BLAST_RADIUS_BOUNDARY
						? ActionAdmissionReliabilityScope.BLAST_RADIUS
						: reason == ActionAdmissionReliabilityReason.MISSING_ROLLBACK_BINDING
						? ActionAdmissionReliabilityScope.ROLLBACK_BOUNDARY
						: reason == ActionAdmissionReliabilityReason.MISSING_VERIFICATION_BINDING
						? ActionAdmissionReliabilityScope.VERIFICATION_BOUNDARY
						: reason == ActionAdmissionReliabilityReason
								.MISSING_HUMAN_APPROVAL_REQUIREMENT
						? ActionAdmissionReliabilityScope.HUMAN_APPROVAL
						: ActionAdmissionReliabilityScope.VERIFICATION,
				reason == ActionAdmissionReliabilityReason.MISSING_ROLLBACK_BINDING
						? verificationReliabilityWithMissingRollback()
						: reason == ActionAdmissionReliabilityReason.MISSING_VERIFICATION_BINDING
						? verificationReliabilityWithMissingVerification()
						: reason == ActionAdmissionReliabilityReason
								.MISSING_HUMAN_APPROVAL_REQUIREMENT
						? verificationReliabilityWithoutHumanApprovalRequirement()
						: blockedVerificationReliability(),
				reason == ActionAdmissionReliabilityReason.MISSING_ACTION_TYPE
						? null
						: ACTION_TYPE,
				reason == ActionAdmissionReliabilityReason.MISSING_BLAST_RADIUS_BOUNDARY
						? null
						: BLAST_RADIUS_BOUNDARY
		);
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

	private VerificationReliability mediumVerificationReliability() {
		return new VerificationReliability(
				VerificationReliabilityLevel.MEDIUM,
				VerificationReliabilityReason.UNKNOWN,
				VerificationReliabilityScope.OPERATOR_VIEW,
				mediumApprovalReliability(),
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

	private ApprovalReliability mediumApprovalReliability() {
		return new ApprovalReliability(
				ApprovalReliabilityLevel.MEDIUM,
				ApprovalReliabilityReason.UNKNOWN,
				ApprovalReliabilityScope.OPERATOR_VIEW,
				mediumRecommendationReliability(),
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

	private RecommendationReliability mediumRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.MEDIUM,
				RecommendationReliabilityReason.UNKNOWN,
				RecommendationReliabilityScope.OPERATOR_VIEW,
				mediumDecisionReliability(),
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

	private DecisionReliability mediumDecisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.MEDIUM,
				DecisionReliabilityReason.UNKNOWN,
				DecisionReliabilityScope.OPERATOR_VIEW,
				mediumAssessmentReliability(),
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

	private AssessmentReliability mediumAssessmentReliability() {
		return assessmentReliability(
				AssessmentReliabilityLevel.MEDIUM,
				AssessmentReliabilityReason.UNKNOWN,
				true,
				evidenceReliability(
						EvidenceReliabilityLevel.MEDIUM,
						EvidenceReliabilityReason.UNKNOWN,
						false,
						true,
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

	private EvidenceConfidence mediumConfidence() {
		EvidenceTrustScore trustScore = trustScore(
				governancePolicy(false, false, false),
				EvidenceLineage.trace(governancePolicy(false, false, false), true, true),
				EvidenceTrustScoreLevel.MEDIUM,
				EvidenceTrustScoreReason.PARTIAL_PROVENANCE
		);
		return new EvidenceConfidence(
				EvidenceConfidenceLevel.MEDIUM,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
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
