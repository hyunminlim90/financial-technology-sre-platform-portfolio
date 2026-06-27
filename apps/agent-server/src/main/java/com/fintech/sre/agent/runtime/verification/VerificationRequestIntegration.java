package com.fintech.sre.agent.runtime.verification;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class VerificationRequestIntegration {

	public VerificationRequestIntegrationResult integrate(
			VerificationRequest verificationRequest
	) {
		if (verificationRequest == null) {
			throw new NullPointerException("verificationRequest must not be null");
		}

		if (verificationRequest.paymentSafetyUncertainty()) {
			return result(
					verificationRequest,
					VerificationRequestIntegrationStatus.BLOCKED,
					VerificationRequestIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					VerificationRequestIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (verificationRequest.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					verificationRequest,
					VerificationRequestIntegrationStatus.BLOCKED,
					VerificationRequestIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					VerificationRequestIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingVerificationRequestIdentifier(verificationRequest)) {
			return result(
					verificationRequest,
					VerificationRequestIntegrationStatus.BLOCKED,
					VerificationRequestIntegrationReason.MISSING_VERIFICATION_REQUEST_IDENTIFIER,
					VerificationRequestIntegrationScope.VERIFICATION_REQUEST,
					false,
					false
			);
		}
		if (missingVerificationPolicy(verificationRequest)) {
			return result(
					verificationRequest,
					VerificationRequestIntegrationStatus.BLOCKED,
					VerificationRequestIntegrationReason.MISSING_VERIFICATION_POLICY,
					VerificationRequestIntegrationScope.VERIFICATION_POLICY,
					false,
					false
			);
		}
		if (!verificationRequest.verificationEvidenceRequired()) {
			return result(
					verificationRequest,
					VerificationRequestIntegrationStatus.BLOCKED,
					VerificationRequestIntegrationReason.MISSING_VERIFICATION_EVIDENCE_REQUIREMENT,
					VerificationRequestIntegrationScope.VERIFICATION_EVIDENCE,
					false,
					false
			);
		}
		if (!verificationRequest.rollbackBindingPresent()) {
			return result(
					verificationRequest,
					VerificationRequestIntegrationStatus.BLOCKED,
					VerificationRequestIntegrationReason.MISSING_ROLLBACK_BINDING,
					VerificationRequestIntegrationScope.ROLLBACK,
					false,
					false
			);
		}

		return switch (verificationRequest.level()) {
			case VERIFICATION_REQUESTABLE -> result(
					verificationRequest,
					VerificationRequestIntegrationStatus.VERIFICATION_REQUEST_READY,
					VerificationRequestIntegrationReason.VERIFICATION_REQUESTABLE,
					VerificationRequestIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					verificationRequest,
					VerificationRequestIntegrationStatus.PARTIAL_VERIFICATION_REQUEST,
					VerificationRequestIntegrationReason.PARTIAL_VERIFICATION_REQUEST,
					VerificationRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					verificationRequest,
					VerificationRequestIntegrationStatus.NOT_READY,
					VerificationRequestIntegrationReason.NOT_READY_VERIFICATION_REQUEST,
					VerificationRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					verificationRequest,
					VerificationRequestIntegrationStatus.UNRELIABLE,
					VerificationRequestIntegrationReason.UNRELIABLE_VERIFICATION_REQUEST,
					VerificationRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					verificationRequest,
					VerificationRequestIntegrationStatus.BLOCKED,
					VerificationRequestIntegrationReason.BLOCKED_VERIFICATION_REQUEST,
					VerificationRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					verificationRequest,
					VerificationRequestIntegrationStatus.UNKNOWN,
					VerificationRequestIntegrationReason.UNKNOWN,
					VerificationRequestIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean verificationRequestGeneration() {
		return false;
	}

	public boolean verificationWorkflow() {
		return false;
	}

	public boolean verificationResult() {
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

	private boolean missingVerificationRequestIdentifier(
			VerificationRequest verificationRequest
	) {
		return verificationRequest.verificationRequestIdentifier() == null
				|| verificationRequest.verificationRequestIdentifier().isBlank();
	}

	private boolean missingVerificationPolicy(VerificationRequest verificationRequest) {
		return verificationRequest.verificationPolicy() == null
				|| verificationRequest.verificationPolicy().isBlank();
	}

	private VerificationRequestIntegrationResult result(
			VerificationRequest verificationRequest,
			VerificationRequestIntegrationStatus status,
			VerificationRequestIntegrationReason reason,
			VerificationRequestIntegrationScope scope,
			boolean workflowEntryReady,
			boolean verificationRequestCertaintyAllowed
	) {
		return new VerificationRequestIntegrationResult(
				verificationRequest,
				status,
				reason,
				scope,
				workflowEntryReady,
				verificationRequestCertaintyAllowed
		);
	}
}
