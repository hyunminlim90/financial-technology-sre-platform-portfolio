package com.fintech.sre.agent.runtime.approval;

public class ApprovalRequestIntegration {

	public ApprovalRequestIntegrationResult integrate(
			ApprovalRequest approvalRequest
	) {
		if (approvalRequest == null) {
			throw new NullPointerException("approvalRequest must not be null");
		}

		if (approvalRequest.paymentSafetyUncertainty()) {
			return result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.BLOCKED,
					ApprovalRequestIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ApprovalRequestIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (approvalRequest.lifecycleRisk() == com.fintech.sre.agent.runtime.reliability.OperationalUncertainty.CRITICAL) {
			return result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.BLOCKED,
					ApprovalRequestIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ApprovalRequestIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingOperatorContext(approvalRequest)) {
			return result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.BLOCKED,
					ApprovalRequestIntegrationReason.MISSING_OPERATOR_CONTEXT,
					ApprovalRequestIntegrationScope.OPERATOR_CONTEXT,
					false,
					false
			);
		}
		if (!approvalRequest.humanApprovalRequired()) {
			return result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.BLOCKED,
					ApprovalRequestIntegrationReason.MISSING_HUMAN_APPROVAL_REQUIREMENT,
					ApprovalRequestIntegrationScope.HUMAN_APPROVAL,
					false,
					false
			);
		}
		if (missingApprovalPolicy(approvalRequest)) {
			return result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.BLOCKED,
					ApprovalRequestIntegrationReason.MISSING_APPROVAL_POLICY,
					ApprovalRequestIntegrationScope.APPROVAL_POLICY,
					false,
					false
			);
		}

		return switch (approvalRequest.level()) {
			case REQUESTABLE -> result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY,
					ApprovalRequestIntegrationReason.REQUESTABLE_APPROVAL_REQUEST,
					ApprovalRequestIntegrationScope.APPROVAL_REQUEST,
					true,
					true
			);
			case PARTIAL -> result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.PARTIAL_APPROVAL_REQUEST,
					ApprovalRequestIntegrationReason.PARTIAL_APPROVAL_REQUEST,
					ApprovalRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.NOT_READY,
					ApprovalRequestIntegrationReason.NOT_READY_APPROVAL_REQUEST,
					ApprovalRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.UNRELIABLE,
					ApprovalRequestIntegrationReason.UNRELIABLE_APPROVAL_REQUEST,
					ApprovalRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.BLOCKED,
					ApprovalRequestIntegrationReason.BLOCKED_APPROVAL_REQUEST,
					ApprovalRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					approvalRequest,
					ApprovalRequestIntegrationStatus.UNKNOWN,
					ApprovalRequestIntegrationReason.UNKNOWN,
					ApprovalRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean approvalRequestGeneration() {
		return false;
	}

	public boolean approvalWorkflow() {
		return false;
	}

	public boolean humanApproval() {
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

	private boolean missingOperatorContext(ApprovalRequest approvalRequest) {
		return approvalRequest.operatorContext() == null
				|| approvalRequest.operatorContext().isBlank();
	}

	private boolean missingApprovalPolicy(ApprovalRequest approvalRequest) {
		return approvalRequest.approvalPolicy() == null
				|| approvalRequest.approvalPolicy().isBlank();
	}

	private ApprovalRequestIntegrationResult result(
			ApprovalRequest approvalRequest,
			ApprovalRequestIntegrationStatus status,
			ApprovalRequestIntegrationReason reason,
			ApprovalRequestIntegrationScope scope,
			boolean workflowEntryReady,
			boolean requestCertaintyAllowed
	) {
		return new ApprovalRequestIntegrationResult(
				approvalRequest,
				status,
				reason,
				scope,
				workflowEntryReady,
				requestCertaintyAllowed
		);
	}
}
