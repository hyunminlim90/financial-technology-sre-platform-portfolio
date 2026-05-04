package com.fintech.sre.agent.model.request;

import java.time.Instant;

public record LogSample(
		Instant timestamp,
		String level,
		String message,
		String traceId
) {
}
