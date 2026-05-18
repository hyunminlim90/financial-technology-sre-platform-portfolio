package com.fintech.sre.agent.learning.promotion;

public class KnowledgePromotionReviewRejectedException extends RuntimeException {

	private final String code;

	public KnowledgePromotionReviewRejectedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
