package com.fintech.sre.agent.runtime.reliability;

public record ActionCommandRequirement(
		boolean rollbackRequirement,
		boolean verificationRequirement,
		boolean humanApprovalSatisfied
) {
	public boolean minimumSafetyRequirementsSatisfied() {
		return rollbackRequirement
				&& verificationRequirement
				&& humanApprovalSatisfied;
	}
}
