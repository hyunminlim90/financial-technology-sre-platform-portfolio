package com.fintech.sre.agent.decision.pipeline;

public class DecisionPipelineException extends RuntimeException {

	private final String stageName;

	public DecisionPipelineException(String stageName, Throwable cause) {
		super("Decision pipeline failed at stage: " + stageName, cause);
		this.stageName = stageName;
	}

	public String stageName() {
		return stageName;
	}
}
