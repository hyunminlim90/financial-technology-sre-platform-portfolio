package com.fintech.sre.agent.runtime.approval;

public class ApprovalStateIntegration {

	public ApprovalStateIntegrationResult integrate(ApprovalState approvalState) {
		if (approvalState == null) {
			throw new NullPointerException("approvalState must not be null");
		}

		if (approvalState.paymentSafetyUncertainty()) {
			return result(
					approvalState,
					ApprovalStateIntegrationStatus.BLOCKED,
					ApprovalStateIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ApprovalStateIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (approvalState.lifecycleRisk()
				== com.fintech.sre.agent.runtime.reliability.OperationalUncertainty.CRITICAL) {
			return result(
					approvalState,
					ApprovalStateIntegrationStatus.BLOCKED,
					ApprovalStateIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ApprovalStateIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingApprovalStateIdentifier(approvalState)) {
			return result(
					approvalState,
					ApprovalStateIntegrationStatus.BLOCKED,
					ApprovalStateIntegrationReason.MISSING_APPROVAL_STATE_IDENTIFIER,
					ApprovalStateIntegrationScope.APPROVAL_STATE,
					false,
					false
			);
		}
		if (missingApprovalPolicy(approvalState)) {
			return result(
					approvalState,
					ApprovalStateIntegrationStatus.BLOCKED,
					ApprovalStateIntegrationReason.MISSING_APPROVAL_POLICY,
					ApprovalStateIntegrationScope.APPROVAL_POLICY,
					false,
					false
			);
		}
		if (missingOperatorContext(approvalState)) {
			return result(
					approvalState,
					ApprovalStateIntegrationStatus.BLOCKED,
					ApprovalStateIntegrationReason.MISSING_OPERATOR_CONTEXT,
					ApprovalStateIntegrationScope.OPERATOR_CONTEXT,
					false,
					false
			);
		}

		return switch (approvalState.level()) {
			case PENDING_APPROVAL -> result(
					approvalState,
					ApprovalStateIntegrationStatus.APPROVAL_PENDING_VIEW,
					ApprovalStateIntegrationReason.PENDING_APPROVAL_STATE,
					ApprovalStateIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					approvalState,
					ApprovalStateIntegrationStatus.PARTIAL_APPROVAL_STATE,
					ApprovalStateIntegrationReason.PARTIAL_APPROVAL_STATE,
					ApprovalStateIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					approvalState,
					ApprovalStateIntegrationStatus.NOT_READY,
					ApprovalStateIntegrationReason.NOT_READY_APPROVAL_STATE,
					ApprovalStateIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					approvalState,
					ApprovalStateIntegrationStatus.UNRELIABLE,
					ApprovalStateIntegrationReason.UNRELIABLE_APPROVAL_STATE,
					ApprovalStateIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					approvalState,
					ApprovalStateIntegrationStatus.BLOCKED,
					ApprovalStateIntegrationReason.BLOCKED_APPROVAL_STATE,
					ApprovalStateIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					approvalState,
					ApprovalStateIntegrationStatus.UNKNOWN,
					ApprovalStateIntegrationReason.UNKNOWN,
					ApprovalStateIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean humanApproval() {
		return false;
	}

	public boolean approvalDecision() {
		return false;
	}

	public boolean approvalWorkflow() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean missingApprovalStateIdentifier(ApprovalState approvalState) {
		return approvalState.approvalStateIdentifier() == null
				|| approvalState.approvalStateIdentifier().isBlank();
	}

	private boolean missingApprovalPolicy(ApprovalState approvalState) {
		return approvalState.approvalPolicy() == null
				|| approvalState.approvalPolicy().isBlank();
	}

	private boolean missingOperatorContext(ApprovalState approvalState) {
		return approvalState.operatorContext() == null
				|| approvalState.operatorContext().isBlank();
	}

	private ApprovalStateIntegrationResult result(
			ApprovalState approvalState,
			ApprovalStateIntegrationStatus status,
			ApprovalStateIntegrationReason reason,
			ApprovalStateIntegrationScope scope,
			boolean operatorFacingPendingApprovalVisible,
			boolean pendingApprovalCertaintyAllowed
	) {
		return new ApprovalStateIntegrationResult(
				approvalState,
				status,
				reason,
				scope,
				operatorFacingPendingApprovalVisible,
				pendingApprovalCertaintyAllowed
		);
	}
}
