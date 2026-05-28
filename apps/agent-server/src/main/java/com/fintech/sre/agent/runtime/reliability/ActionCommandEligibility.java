package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record ActionCommandEligibility(
		boolean eligible,
		ActionCommandRequirement requirement,
		List<ActionCommandRestriction> restrictions,
		List<ActionCommandBoundaryReason> reasons
) {
	public ActionCommandEligibility {
		Objects.requireNonNull(requirement, "requirement must not be null");
		Objects.requireNonNull(
				restrictions,
				"restrictions must not be null"
		);
		Objects.requireNonNull(reasons, "reasons must not be null");
		restrictions = List.copyOf(restrictions);
		reasons = List.copyOf(reasons);
	}

	public boolean semanticAdmissionOnly() {
		return true;
	}

	public boolean executable() {
		return false;
	}
}
