package com.fintech.sre.agent.learning.candidate;

public class LearningCandidateRejectedException
		extends RuntimeException {

	private final String code;

	public LearningCandidateRejectedException(
			String code,
			String message
	) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
