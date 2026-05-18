package com.fintech.sre.agent.action;

public record RollbackPolicy(
		boolean required
) {
	public static RollbackPolicy requiredPolicy() {
		return new RollbackPolicy(true);
	}
}
