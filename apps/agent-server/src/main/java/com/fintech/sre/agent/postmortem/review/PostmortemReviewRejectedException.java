package com.fintech.sre.agent.postmortem.review;

public class PostmortemReviewRejectedException
		extends RuntimeException {

	private final String code;

	public PostmortemReviewRejectedException(
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
