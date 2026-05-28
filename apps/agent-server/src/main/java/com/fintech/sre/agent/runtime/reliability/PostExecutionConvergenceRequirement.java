package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record PostExecutionConvergenceRequirement(
		PostExecutionVerificationDecision verificationDecision,
		ConvergenceWindow convergenceWindow,
		PropagationSignal propagationSignal,
		boolean propagationActive
) {
	public PostExecutionConvergenceRequirement {
		Objects.requireNonNull(
				verificationDecision,
				"verificationDecision must not be null"
		);
		Objects.requireNonNull(
				convergenceWindow,
				"convergenceWindow must not be null"
		);
		Objects.requireNonNull(
				propagationSignal,
				"propagationSignal must not be null"
		);
	}

	public PostExecutionVerificationRequirement verificationRequirement() {
		return verificationDecision.requirement();
	}
}
