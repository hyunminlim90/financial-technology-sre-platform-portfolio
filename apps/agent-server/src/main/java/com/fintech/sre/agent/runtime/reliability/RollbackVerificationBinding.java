package com.fintech.sre.agent.runtime.reliability;

public class RollbackVerificationBinding {

	public RollbackVerificationBindingDecision bind(
			RollbackReference rollbackReference,
			VerificationReference verificationReference,
			boolean paymentSafetyAction
	) {
		if (rollbackReference == null) {
			return rejected(
					null,
					verificationReference,
					RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE
			);
		}
		if (verificationReference == null) {
			return rejected(
					rollbackReference,
					null,
					RollbackVerificationBindingRejectionReason
							.MISSING_VERIFICATION_REFERENCE
			);
		}
		if (!rollbackReference.known()) {
			return rejected(
					rollbackReference,
					verificationReference,
					RollbackVerificationBindingRejectionReason
							.UNKNOWN_ROLLBACK_REFERENCE
			);
		}
		if (!verificationReference.known()) {
			return rejected(
					rollbackReference,
					verificationReference,
					RollbackVerificationBindingRejectionReason
							.UNKNOWN_VERIFICATION_REFERENCE
			);
		}
		if (paymentSafetyAction
				&& !verificationReference.paymentConsistencyVerification()) {
			return rejected(
					rollbackReference,
					verificationReference,
					RollbackVerificationBindingRejectionReason
							.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
			);
		}
		if (rollbackReference.deprecated()) {
			return restricted(
					rollbackReference,
					verificationReference,
					RollbackVerificationBindingRejectionReason
							.DEPRECATED_ROLLBACK_HIGH_RISK_RESTRICTION
			);
		}
		if (verificationReference.deprecated()) {
			return restricted(
					rollbackReference,
					verificationReference,
					RollbackVerificationBindingRejectionReason
							.DEPRECATED_VERIFICATION_HIGH_RISK_RESTRICTION
			);
		}
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.BOUND,
				rollbackReference,
				verificationReference,
				null
		);
	}

	private RollbackVerificationBindingDecision rejected(
			RollbackReference rollbackReference,
			VerificationReference verificationReference,
			RollbackVerificationBindingRejectionReason rejectionReason
	) {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.REJECTED,
				rollbackReference,
				verificationReference,
				rejectionReason
		);
	}

	private RollbackVerificationBindingDecision restricted(
			RollbackReference rollbackReference,
			VerificationReference verificationReference,
			RollbackVerificationBindingRejectionReason rejectionReason
	) {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.RESTRICTED,
				rollbackReference,
				verificationReference,
				rejectionReason
		);
	}
}
