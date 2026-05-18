package com.fintech.sre.agent.postmortem.review;

public record PostmortemReviewErrorResponse(
		String code,
		String message
) {
}
