package com.fintech.sre.agent.recommendation.approval;

public class RecommendationApprovalRejectedException extends RuntimeException {

	private final String code;

	public RecommendationApprovalRejectedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
