package com.fintech.sre.agent.runtime.reliability;

public record RuntimeTransitionDecision(
		boolean allowed,
		RuntimeStateTransition transition,
		RuntimeTransitionRejectionReason rejectionReason
) {
	public RuntimeTransitionDecision {
		if (!allowed && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected transition decision requires rejection reason"
			);
		}
		if (allowed && rejectionReason != null) {
			throw new IllegalArgumentException(
					"allowed transition decision must not contain rejection reason"
			);
		}
		if (!allowed && transition != null) {
			throw new IllegalArgumentException(
					"rejected transition decision must not contain transition"
			);
		}
	}

	public static RuntimeTransitionDecision allowed(
			RuntimeStateTransition transition
	) {
		return new RuntimeTransitionDecision(true, transition, null);
	}

	public static RuntimeTransitionDecision rejected(
			RuntimeTransitionRejectionReason rejectionReason
	) {
		return new RuntimeTransitionDecision(false, null, rejectionReason);
	}
}
