package com.fintech.sre.agent.runtime.approval;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ApprovalDecisionEvaluator {

	public ApprovalDecision evaluate(
			ApprovalStateIntegrationResult approvalStateIntegration,
			String decisionIdentifier,
			String approvalPolicy,
			String operatorContext,
			boolean decisionRationaleRequired,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				approvalStateIntegration,
				"approvalStateIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ApprovalDecision(
				level(
						approvalStateIntegration,
						decisionIdentifier,
						approvalPolicy,
						operatorContext,
						decisionRationaleRequired,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						approvalStateIntegration,
						decisionIdentifier,
						approvalPolicy,
						operatorContext,
						decisionRationaleRequired,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						approvalStateIntegration,
						decisionIdentifier,
						approvalPolicy,
						operatorContext,
						decisionRationaleRequired,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				approvalStateIntegration,
				decisionIdentifier,
				approvalPolicy,
				operatorContext,
				decisionRationaleRequired,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ApprovalDecisionLevel level(
			ApprovalStateIntegrationResult approvalStateIntegration,
			String decisionIdentifier,
			String approvalPolicy,
			String operatorContext,
			boolean decisionRationaleRequired,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ApprovalDecisionLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ApprovalDecisionLevel.BLOCKED;
		}
		if (missingDecisionIdentifier(decisionIdentifier)) {
			return ApprovalDecisionLevel.BLOCKED;
		}
		if (missingApprovalPolicy(approvalPolicy)) {
			return ApprovalDecisionLevel.BLOCKED;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalDecisionLevel.BLOCKED;
		}
		if (!decisionRationaleRequired) {
			return ApprovalDecisionLevel.BLOCKED;
		}
		return switch (approvalStateIntegration.status()) {
			case APPROVAL_PENDING_VIEW -> ApprovalDecisionLevel.DECISION_PENDING;
			case PARTIAL_APPROVAL_STATE -> ApprovalDecisionLevel.PARTIAL;
			case NOT_READY -> ApprovalDecisionLevel.NOT_READY;
			case UNRELIABLE -> ApprovalDecisionLevel.UNRELIABLE;
			case BLOCKED -> ApprovalDecisionLevel.BLOCKED;
			case UNKNOWN -> ApprovalDecisionLevel.UNKNOWN;
		};
	}

	private ApprovalDecisionReason reason(
			ApprovalStateIntegrationResult approvalStateIntegration,
			String decisionIdentifier,
			String approvalPolicy,
			String operatorContext,
			boolean decisionRationaleRequired,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ApprovalDecisionReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ApprovalDecisionReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingDecisionIdentifier(decisionIdentifier)) {
			return ApprovalDecisionReason.MISSING_DECISION_IDENTIFIER;
		}
		if (missingApprovalPolicy(approvalPolicy)) {
			return ApprovalDecisionReason.MISSING_APPROVAL_POLICY;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalDecisionReason.MISSING_OPERATOR_CONTEXT;
		}
		if (!decisionRationaleRequired) {
			return ApprovalDecisionReason.MISSING_DECISION_RATIONALE_REQUIREMENT;
		}
		return switch (approvalStateIntegration.status()) {
			case APPROVAL_PENDING_VIEW -> ApprovalDecisionReason.APPROVAL_PENDING_VIEW;
			case PARTIAL_APPROVAL_STATE -> ApprovalDecisionReason.PARTIAL_APPROVAL_STATE;
			case NOT_READY -> ApprovalDecisionReason.NOT_READY_APPROVAL_STATE;
			case UNRELIABLE -> ApprovalDecisionReason.UNRELIABLE_APPROVAL_STATE;
			case BLOCKED -> ApprovalDecisionReason.BLOCKED_APPROVAL_STATE;
			case UNKNOWN -> ApprovalDecisionReason.UNKNOWN;
		};
	}

	private ApprovalDecisionScope scope(
			ApprovalStateIntegrationResult approvalStateIntegration,
			String decisionIdentifier,
			String approvalPolicy,
			String operatorContext,
			boolean decisionRationaleRequired,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ApprovalDecisionScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ApprovalDecisionScope.LIFECYCLE_RISK;
		}
		if (missingDecisionIdentifier(decisionIdentifier)) {
			return ApprovalDecisionScope.APPROVAL_DECISION;
		}
		if (missingApprovalPolicy(approvalPolicy)) {
			return ApprovalDecisionScope.APPROVAL_POLICY;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalDecisionScope.OPERATOR_CONTEXT;
		}
		if (!decisionRationaleRequired) {
			return ApprovalDecisionScope.DECISION_RATIONALE;
		}
		return ApprovalDecisionScope.APPROVAL_STATE;
	}

	private boolean missingDecisionIdentifier(String decisionIdentifier) {
		return decisionIdentifier == null || decisionIdentifier.isBlank();
	}

	private boolean missingApprovalPolicy(String approvalPolicy) {
		return approvalPolicy == null || approvalPolicy.isBlank();
	}

	private boolean missingOperatorContext(String operatorContext) {
		return operatorContext == null || operatorContext.isBlank();
	}
}
