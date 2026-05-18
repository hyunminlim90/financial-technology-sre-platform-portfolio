package com.fintech.sre.agent.recommendation.verification;

public class VerificationResultRejectedException
		extends RuntimeException {

	private final String code;

	public VerificationResultRejectedException(
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
