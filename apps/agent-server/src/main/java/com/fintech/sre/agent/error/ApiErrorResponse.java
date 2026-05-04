package com.fintech.sre.agent.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		String errorId,
		String message,
		List<ErrorDetail> details,
		String humanActionRequired,
		Instant timestamp
) {
}
