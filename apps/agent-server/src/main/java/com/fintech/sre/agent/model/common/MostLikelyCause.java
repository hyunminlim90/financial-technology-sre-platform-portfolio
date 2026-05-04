package com.fintech.sre.agent.model.common;

public record MostLikelyCause(
		String cause,
		ConfidenceLevel confidence,
		String reason
) {
}
