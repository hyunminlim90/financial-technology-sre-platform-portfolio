package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record RollbackVerificationBindingDecision(
		RollbackVerificationBindingStatus status,
		RollbackReference rollbackReference,
		VerificationReference verificationReference,
		RollbackVerificationBindingRejectionReason rejectionReason
) {
	public RollbackVerificationBindingDecision {
		Objects.requireNonNull(status, "status must not be null");
		if (status == RollbackVerificationBindingStatus.REJECTED
				&& rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected rollback verification binding requires rejection reason"
			);
		}
		if (status == RollbackVerificationBindingStatus.BOUND
				&& rejectionReason != null) {
			throw new IllegalArgumentException(
					"bound rollback verification binding must not contain rejection reason"
			);
		}
	}

	public boolean actionCommandAvailable() {
		return status == RollbackVerificationBindingStatus.BOUND
				|| status == RollbackVerificationBindingStatus.RESTRICTED;
	}

	public boolean highRiskRestricted() {
		return status == RollbackVerificationBindingStatus.RESTRICTED;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean semanticPrerequisiteOnly() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
