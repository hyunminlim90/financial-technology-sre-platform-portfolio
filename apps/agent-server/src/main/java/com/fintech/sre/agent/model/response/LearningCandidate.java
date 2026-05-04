package com.fintech.sre.agent.model.response;

public record LearningCandidate(
		String type,
		String title,
		String reason,
		String suggestedPath,
		String priority
) {
}
