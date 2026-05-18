package com.fintech.sre.agent.learning.application;

public class KnowledgeUpdateApplicationRejectedException
		extends RuntimeException {

	private final String code;

	public KnowledgeUpdateApplicationRejectedException(
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
