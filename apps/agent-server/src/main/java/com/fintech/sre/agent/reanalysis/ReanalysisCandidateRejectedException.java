package com.fintech.sre.agent.reanalysis;

public class ReanalysisCandidateRejectedException
		extends RuntimeException {

	private final String code;

	public ReanalysisCandidateRejectedException(
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
