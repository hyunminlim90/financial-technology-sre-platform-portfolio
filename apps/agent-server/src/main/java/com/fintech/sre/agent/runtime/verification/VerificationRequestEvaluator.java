package com.fintech.sre.agent.runtime.verification;

import java.util.Objects;

import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationResult;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationStatus;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class VerificationRequestEvaluator {

	public VerificationRequest evaluate(
			ApprovalDecisionIntegrationResult approvalDecisionIntegration,
			String verificationRequestIdentifier,
			String verificationPolicy,
			boolean verificationEvidenceRequired,
			boolean rollbackBindingPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				approvalDecisionIntegration,
				"approvalDecisionIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new VerificationRequest(
				level(
						approvalDecisionIntegration,
						verificationRequestIdentifier,
						verificationPolicy,
						verificationEvidenceRequired,
						rollbackBindingPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						approvalDecisionIntegration,
						verificationRequestIdentifier,
						verificationPolicy,
						verificationEvidenceRequired,
						rollbackBindingPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						approvalDecisionIntegration,
						verificationRequestIdentifier,
						verificationPolicy,
						verificationEvidenceRequired,
						rollbackBindingPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				approvalDecisionIntegration,
				verificationRequestIdentifier,
				verificationPolicy,
				verificationEvidenceRequired,
				rollbackBindingPresent,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private VerificationRequestLevel level(
			ApprovalDecisionIntegrationResult approvalDecisionIntegration,
			String verificationRequestIdentifier,
			String verificationPolicy,
			boolean verificationEvidenceRequired,
			boolean rollbackBindingPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return VerificationRequestLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return VerificationRequestLevel.BLOCKED;
		}
		if (missingVerificationRequestIdentifier(verificationRequestIdentifier)) {
			return VerificationRequestLevel.BLOCKED;
		}
		if (missingVerificationPolicy(verificationPolicy)) {
			return VerificationRequestLevel.BLOCKED;
		}
		if (!verificationEvidenceRequired) {
			return VerificationRequestLevel.BLOCKED;
		}
		if (!rollbackBindingPresent) {
			return VerificationRequestLevel.BLOCKED;
		}
		return switch (approvalDecisionIntegration.status()) {
			case APPROVAL_DECISION_PENDING_VIEW -> VerificationRequestLevel.VERIFICATION_REQUESTABLE;
			case PARTIAL_APPROVAL_DECISION -> VerificationRequestLevel.PARTIAL;
			case NOT_READY -> VerificationRequestLevel.NOT_READY;
			case UNRELIABLE -> VerificationRequestLevel.UNRELIABLE;
			case BLOCKED -> VerificationRequestLevel.BLOCKED;
			case UNKNOWN -> VerificationRequestLevel.UNKNOWN;
		};
	}

	private VerificationRequestReason reason(
			ApprovalDecisionIntegrationResult approvalDecisionIntegration,
			String verificationRequestIdentifier,
			String verificationPolicy,
			boolean verificationEvidenceRequired,
			boolean rollbackBindingPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return VerificationRequestReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return VerificationRequestReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingVerificationRequestIdentifier(verificationRequestIdentifier)) {
			return VerificationRequestReason.MISSING_VERIFICATION_REQUEST_IDENTIFIER;
		}
		if (missingVerificationPolicy(verificationPolicy)) {
			return VerificationRequestReason.MISSING_VERIFICATION_POLICY;
		}
		if (!verificationEvidenceRequired) {
			return VerificationRequestReason.MISSING_VERIFICATION_EVIDENCE_REQUIREMENT;
		}
		if (!rollbackBindingPresent) {
			return VerificationRequestReason.MISSING_ROLLBACK_BINDING;
		}
		return switch (approvalDecisionIntegration.status()) {
			case APPROVAL_DECISION_PENDING_VIEW -> VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW;
			case PARTIAL_APPROVAL_DECISION -> VerificationRequestReason.PARTIAL_APPROVAL_DECISION;
			case NOT_READY -> VerificationRequestReason.NOT_READY_APPROVAL_DECISION;
			case UNRELIABLE -> VerificationRequestReason.UNRELIABLE_APPROVAL_DECISION;
			case BLOCKED -> VerificationRequestReason.BLOCKED_APPROVAL_DECISION;
			case UNKNOWN -> VerificationRequestReason.UNKNOWN;
		};
	}

	private VerificationRequestScope scope(
			ApprovalDecisionIntegrationResult approvalDecisionIntegration,
			String verificationRequestIdentifier,
			String verificationPolicy,
			boolean verificationEvidenceRequired,
			boolean rollbackBindingPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return VerificationRequestScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return VerificationRequestScope.LIFECYCLE_RISK;
		}
		if (missingVerificationRequestIdentifier(verificationRequestIdentifier)) {
			return VerificationRequestScope.VERIFICATION_REQUEST;
		}
		if (missingVerificationPolicy(verificationPolicy)) {
			return VerificationRequestScope.VERIFICATION_POLICY;
		}
		if (!verificationEvidenceRequired) {
			return VerificationRequestScope.VERIFICATION_EVIDENCE;
		}
		if (!rollbackBindingPresent) {
			return VerificationRequestScope.ROLLBACK;
		}
		return VerificationRequestScope.APPROVAL_DECISION;
	}

	private boolean missingVerificationRequestIdentifier(
			String verificationRequestIdentifier
	) {
		return verificationRequestIdentifier == null
				|| verificationRequestIdentifier.isBlank();
	}

	private boolean missingVerificationPolicy(String verificationPolicy) {
		return verificationPolicy == null || verificationPolicy.isBlank();
	}
}
