package com.fintech.sre.agent.recommendation.execution.result;

public class HumanExecutionResultRejectedException extends RuntimeException {

	private final String code;

	public HumanExecutionResultRejectedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
