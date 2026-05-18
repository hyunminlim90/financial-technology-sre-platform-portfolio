package com.fintech.sre.agent.recommendation.execution;

public class ExecutionPlanRejectedException extends RuntimeException {

	private final String code;

	public ExecutionPlanRejectedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
