package com.fintech.sre.agent.model.common;

public record ForbiddenAction(
		String action,
		String reason
) {
}
