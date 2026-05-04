package com.fintech.sre.agent.model.response;

import java.time.Instant;

public record ApiErrorResponse(
		String status,
		String errorCode,
		String message,
		Boolean humanEscalationRequired,
		Instant timestamp
) {
}
