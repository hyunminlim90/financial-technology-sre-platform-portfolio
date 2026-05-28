package com.fintech.sre.agent.runtime.reliability;

import java.time.Instant;
import java.util.Objects;

public record LifecycleAuditEvent(
		LifecycleAuditEventType type,
		String eventId,
		String summary,
		Instant recordedAt
) {
	public LifecycleAuditEvent {
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(eventId, "eventId must not be null");
		Objects.requireNonNull(summary, "summary must not be null");
		Objects.requireNonNull(recordedAt, "recordedAt must not be null");
	}
}
