package com.fintech.sre.agent.runtime.reliability;

import java.time.Instant;
import java.util.Objects;

public class RuntimeStateMachine {

	private final RuntimeTransitionGuard transitionGuard;

	public RuntimeStateMachine(RuntimeTransitionGuard transitionGuard) {
		this.transitionGuard = Objects.requireNonNull(
				transitionGuard,
				"transitionGuard must not be null"
		);
	}

	public RuntimeTransitionDecision transition(
			RuntimeState from,
			RuntimeState to,
			ReliabilityAssessment assessment,
			String reason,
			Instant transitionedAt
	) {
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(transitionedAt, "transitionedAt must not be null");

		RuntimeTransitionDecision guardDecision = transitionGuard.evaluate(
				from,
				to,
				assessment
		);
		if (!guardDecision.allowed()) {
			return guardDecision;
		}

		return RuntimeTransitionDecision.allowed(
				new RuntimeStateTransition(from, to, reason, transitionedAt)
		);
	}
}
