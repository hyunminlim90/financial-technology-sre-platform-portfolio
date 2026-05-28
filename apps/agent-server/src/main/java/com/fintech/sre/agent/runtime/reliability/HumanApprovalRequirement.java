package com.fintech.sre.agent.runtime.reliability;

public record HumanApprovalRequirement(
		boolean explicitHumanApprovalRequired,
		boolean verificationReviewRequired,
		boolean rollbackReviewRequired,
		boolean aiOnlyApprovalAllowed
) {
	public boolean humanReviewRequired() {
		return explicitHumanApprovalRequired
				|| verificationReviewRequired
				|| rollbackReviewRequired;
	}
}
