package com.fintech.sre.agent.observability.model;

import java.time.Instant;

public record LogEvidence(
		Instant timestamp,
		String level,
		String message,
		String traceId,
		String query,
		String source
) {
}
