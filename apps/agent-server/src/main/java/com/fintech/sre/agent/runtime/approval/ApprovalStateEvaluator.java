package com.fintech.sre.agent.runtime.approval;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ApprovalStateEvaluator {

	public ApprovalState evaluate(
			ApprovalRequestIntegrationResult approvalRequestIntegration,
			String approvalStateIdentifier,
			String approvalPolicy,
			String operatorContext,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				approvalRequestIntegration,
				"approvalRequestIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ApprovalState(
				level(
						approvalRequestIntegration,
						approvalStateIdentifier,
						approvalPolicy,
						operatorContext,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						approvalRequestIntegration,
						approvalStateIdentifier,
						approvalPolicy,
						operatorContext,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						approvalRequestIntegration,
						approvalStateIdentifier,
						approvalPolicy,
						operatorContext,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				approvalRequestIntegration,
				approvalStateIdentifier,
				approvalPolicy,
				operatorContext,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ApprovalStateLevel level(
			ApprovalRequestIntegrationResult approvalRequestIntegration,
			String approvalStateIdentifier,
			String approvalPolicy,
			String operatorContext,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ApprovalStateLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ApprovalStateLevel.BLOCKED;
		}
		if (missingApprovalStateIdentifier(approvalStateIdentifier)) {
			return ApprovalStateLevel.BLOCKED;
		}
		if (missingApprovalPolicy(approvalPolicy)) {
			return ApprovalStateLevel.BLOCKED;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalStateLevel.BLOCKED;
		}
		return switch (approvalRequestIntegration.status()) {
			case APPROVAL_REQUEST_READY -> ApprovalStateLevel.PENDING_APPROVAL;
			case PARTIAL_APPROVAL_REQUEST -> ApprovalStateLevel.PARTIAL;
			case NOT_READY -> ApprovalStateLevel.NOT_READY;
			case UNRELIABLE -> ApprovalStateLevel.UNRELIABLE;
			case BLOCKED -> ApprovalStateLevel.BLOCKED;
			case UNKNOWN -> ApprovalStateLevel.UNKNOWN;
		};
	}

	private ApprovalStateReason reason(
			ApprovalRequestIntegrationResult approvalRequestIntegration,
			String approvalStateIdentifier,
			String approvalPolicy,
			String operatorContext,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ApprovalStateReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ApprovalStateReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingApprovalStateIdentifier(approvalStateIdentifier)) {
			return ApprovalStateReason.MISSING_APPROVAL_STATE_IDENTIFIER;
		}
		if (missingApprovalPolicy(approvalPolicy)) {
			return ApprovalStateReason.MISSING_APPROVAL_POLICY;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalStateReason.MISSING_OPERATOR_CONTEXT;
		}
		return switch (approvalRequestIntegration.status()) {
			case APPROVAL_REQUEST_READY -> ApprovalStateReason.APPROVAL_REQUEST_READY;
			case PARTIAL_APPROVAL_REQUEST -> ApprovalStateReason.PARTIAL_APPROVAL_REQUEST;
			case NOT_READY -> ApprovalStateReason.NOT_READY_APPROVAL_REQUEST;
			case UNRELIABLE -> ApprovalStateReason.UNRELIABLE_APPROVAL_REQUEST;
			case BLOCKED -> ApprovalStateReason.BLOCKED_APPROVAL_REQUEST;
			case UNKNOWN -> ApprovalStateReason.UNKNOWN;
		};
	}

	private ApprovalStateScope scope(
			ApprovalRequestIntegrationResult approvalRequestIntegration,
			String approvalStateIdentifier,
			String approvalPolicy,
			String operatorContext,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ApprovalStateScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ApprovalStateScope.LIFECYCLE_RISK;
		}
		if (missingApprovalStateIdentifier(approvalStateIdentifier)) {
			return ApprovalStateScope.APPROVAL_STATE;
		}
		if (missingApprovalPolicy(approvalPolicy)) {
			return ApprovalStateScope.APPROVAL_POLICY;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalStateScope.OPERATOR_CONTEXT;
		}
		return ApprovalStateScope.APPROVAL_REQUEST;
	}

	private boolean missingApprovalStateIdentifier(String approvalStateIdentifier) {
		return approvalStateIdentifier == null || approvalStateIdentifier.isBlank();
	}

	private boolean missingApprovalPolicy(String approvalPolicy) {
		return approvalPolicy == null || approvalPolicy.isBlank();
	}

	private boolean missingOperatorContext(String operatorContext) {
		return operatorContext == null || operatorContext.isBlank();
	}
}
