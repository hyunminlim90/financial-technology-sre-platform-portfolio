package com.fintech.sre.agent.runtime.reliability;

import java.time.Instant;
import java.util.Objects;

public record RuntimeStateTransition(
		RuntimeState from,
		RuntimeState to,
		String reason,
		Instant transitionedAt
) {
	public RuntimeStateTransition {
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(to, "to must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(transitionedAt, "transitionedAt must not be null");

		if (reason.isBlank()) {
			throw new IllegalArgumentException("reason must not be blank");
		}
		if (!from.canTransitionTo(to)) {
			throw new IllegalArgumentException("invalid runtime state transition");
		}
	}

	public boolean isRollbackPath() {
		return from == RuntimeState.ROLLING_BACK || to == RuntimeState.ROLLING_BACK;
	}
}
