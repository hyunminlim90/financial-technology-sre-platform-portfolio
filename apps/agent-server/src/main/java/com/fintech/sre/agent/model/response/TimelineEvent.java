package com.fintech.sre.agent.model.response;

import java.time.Instant;

public record TimelineEvent(
		Instant time,
		String event,
		String source
) {
}
