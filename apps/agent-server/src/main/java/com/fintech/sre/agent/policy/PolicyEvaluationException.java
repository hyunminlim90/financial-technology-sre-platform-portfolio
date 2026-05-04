package com.fintech.sre.agent.policy;

import java.util.List;

public class PolicyEvaluationException extends RuntimeException {

	private final List<PolicyViolation> violations;

	public PolicyEvaluationException(List<PolicyViolation> violations) {
		super("Policy evaluation failed");
		this.violations = violations;
	}

	public List<PolicyViolation> violations() {
		return violations;
	}
}
