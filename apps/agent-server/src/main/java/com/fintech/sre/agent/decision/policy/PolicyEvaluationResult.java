package com.fintech.sre.agent.decision.policy;

import java.util.List;

public record PolicyEvaluationResult(
		PolicyEvaluationDecision decision,
		List<PolicyViolation> violations
) {

	public static PolicyEvaluationResult allow() {
		return new PolicyEvaluationResult(PolicyEvaluationDecision.ALLOW, List.of());
	}

	public static PolicyEvaluationResult requireApproval(String policyId, String message) {
		return new PolicyEvaluationResult(
				PolicyEvaluationDecision.REQUIRE_APPROVAL,
				List.of(new PolicyViolation(policyId, message))
		);
	}

	public static PolicyEvaluationResult deny(String policyId, String message) {
		return new PolicyEvaluationResult(
				PolicyEvaluationDecision.DENY,
				List.of(new PolicyViolation(policyId, message))
		);
	}

	public static PolicyEvaluationResult insufficientEvidence(String policyId, String message) {
		return new PolicyEvaluationResult(
				PolicyEvaluationDecision.INSUFFICIENT_EVIDENCE,
				List.of(new PolicyViolation(policyId, message))
		);
	}

	public boolean allowed() {
		return decision == PolicyEvaluationDecision.ALLOW
				|| decision == PolicyEvaluationDecision.REQUIRE_APPROVAL;
	}

	public boolean denied() {
		return decision == PolicyEvaluationDecision.DENY;
	}

	public String reason() {
		return violations.isEmpty() ? null : violations.get(0).message();
	}
}
