package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record HumanApprovalDecision(
		boolean approvalRequired,
		HumanApprovalScope scope,
		HumanApprovalRequirement requirement,
		List<HumanApprovalReason> reasons
) {
	public HumanApprovalDecision {
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(requirement, "requirement must not be null");
		Objects.requireNonNull(reasons, "reasons must not be null");
		reasons = List.copyOf(reasons);
	}

	public boolean semanticGovernanceOnly() {
		return true;
	}

	public boolean executionAuthorityGranted() {
		return false;
	}
}
