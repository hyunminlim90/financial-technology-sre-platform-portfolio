package com.fintech.sre.agent.action;

public record ApprovalPolicy(
		boolean required
) {
	public static ApprovalPolicy humanRequired() {
		return new ApprovalPolicy(true);
	}
}
