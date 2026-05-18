package com.fintech.sre.agent.admin.knowledge;

public class KnowledgeIngestionRejectedException extends RuntimeException {

	private final String code;

	public KnowledgeIngestionRejectedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
