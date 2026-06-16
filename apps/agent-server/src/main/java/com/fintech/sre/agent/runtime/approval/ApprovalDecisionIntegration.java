package com.fintech.sre.agent.runtime.approval;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ApprovalDecisionIntegration {

	public ApprovalDecisionIntegrationResult integrate(
			ApprovalDecision approvalDecision
	) {
		if (approvalDecision == null) {
			throw new NullPointerException("approvalDecision must not be null");
		}

		if (approvalDecision.paymentSafetyUncertainty()) {
			return result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.BLOCKED,
					ApprovalDecisionIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ApprovalDecisionIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (approvalDecision.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.BLOCKED,
					ApprovalDecisionIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ApprovalDecisionIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingDecisionIdentifier(approvalDecision)) {
			return result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.BLOCKED,
					ApprovalDecisionIntegrationReason.MISSING_DECISION_IDENTIFIER,
					ApprovalDecisionIntegrationScope.APPROVAL_DECISION,
					false,
					false
			);
		}
		if (missingApprovalPolicy(approvalDecision)) {
			return result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.BLOCKED,
					ApprovalDecisionIntegrationReason.MISSING_APPROVAL_POLICY,
					ApprovalDecisionIntegrationScope.APPROVAL_POLICY,
					false,
					false
			);
		}
		if (missingOperatorContext(approvalDecision)) {
			return result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.BLOCKED,
					ApprovalDecisionIntegrationReason.MISSING_OPERATOR_CONTEXT,
					ApprovalDecisionIntegrationScope.OPERATOR_CONTEXT,
					false,
					false
			);
		}
		if (!approvalDecision.decisionRationaleRequired()) {
			return result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.BLOCKED,
					ApprovalDecisionIntegrationReason.MISSING_DECISION_RATIONALE_REQUIREMENT,
					ApprovalDecisionIntegrationScope.DECISION_RATIONALE,
					false,
					false
			);
		}

		return switch (approvalDecision.level()) {
			case DECISION_PENDING -> result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.APPROVAL_DECISION_PENDING_VIEW,
					ApprovalDecisionIntegrationReason.DECISION_PENDING,
					ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.PARTIAL_APPROVAL_DECISION,
					ApprovalDecisionIntegrationReason.PARTIAL_APPROVAL_DECISION,
					ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.NOT_READY,
					ApprovalDecisionIntegrationReason.NOT_READY_APPROVAL_DECISION,
					ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.UNRELIABLE,
					ApprovalDecisionIntegrationReason.UNRELIABLE_APPROVAL_DECISION,
					ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.BLOCKED,
					ApprovalDecisionIntegrationReason.BLOCKED_APPROVAL_DECISION,
					ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					approvalDecision,
					ApprovalDecisionIntegrationStatus.UNKNOWN,
					ApprovalDecisionIntegrationReason.UNKNOWN,
					ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
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

	public boolean approvalResult() {
		return false;
	}

	public boolean approvalWorkflow() {
		return false;
	}

	public boolean verificationRequest() {
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

	private boolean missingDecisionIdentifier(ApprovalDecision approvalDecision) {
		return approvalDecision.decisionIdentifier() == null
				|| approvalDecision.decisionIdentifier().isBlank();
	}

	private boolean missingApprovalPolicy(ApprovalDecision approvalDecision) {
		return approvalDecision.approvalPolicy() == null
				|| approvalDecision.approvalPolicy().isBlank();
	}

	private boolean missingOperatorContext(ApprovalDecision approvalDecision) {
		return approvalDecision.operatorContext() == null
				|| approvalDecision.operatorContext().isBlank();
	}

	private ApprovalDecisionIntegrationResult result(
			ApprovalDecision approvalDecision,
			ApprovalDecisionIntegrationStatus status,
			ApprovalDecisionIntegrationReason reason,
			ApprovalDecisionIntegrationScope scope,
			boolean operatorFacingDecisionPendingVisible,
			boolean decisionPendingCertaintyAllowed
	) {
		return new ApprovalDecisionIntegrationResult(
				approvalDecision,
				status,
				reason,
				scope,
				operatorFacingDecisionPendingVisible,
				decisionPendingCertaintyAllowed
		);
	}
}
