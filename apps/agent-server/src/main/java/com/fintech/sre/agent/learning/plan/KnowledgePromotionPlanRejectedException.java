package com.fintech.sre.agent.learning.plan;

public class KnowledgePromotionPlanRejectedException extends RuntimeException {

	private final String code;

	public KnowledgePromotionPlanRejectedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
