package com.fintech.sre.agent.runtime.reliability;

public enum RuntimeState {
	UNKNOWN,
	NORMAL,
	DEGRADED,
	PROPAGATING,
	VERIFYING,
	VERIFIED,
	ROLLING_BACK,
	ROLLBACKING,
	RECOVERING,
	UNSTABLE,
	CONVERGED,
	FAILED;

	public boolean canTransitionTo(RuntimeState target) {
		if (target == null || target == this) {
			return false;
		}

		return switch (this) {
			case UNKNOWN -> target == DEGRADED
					|| target == PROPAGATING
					|| target == VERIFYING
					|| target == CONVERGED;
			case NORMAL -> target == DEGRADED;
			case DEGRADED -> target == VERIFYING
					|| target == PROPAGATING
					|| target == ROLLING_BACK
					|| target == ROLLBACKING
					|| target == RECOVERING
					|| target == UNSTABLE;
			case PROPAGATING -> target == VERIFYING
					|| target == ROLLBACKING
					|| target == DEGRADED
					|| target == UNSTABLE
					|| target == CONVERGED
					|| target == FAILED;
			case VERIFYING -> target == DEGRADED
					|| target == VERIFIED
					|| target == ROLLING_BACK
					|| target == ROLLBACKING
					|| target == RECOVERING
					|| target == UNSTABLE
					|| target == FAILED;
			case VERIFIED -> target == CONVERGED
					|| target == DEGRADED
					|| target == FAILED;
			case ROLLING_BACK -> target == RECOVERING
					|| target == UNSTABLE;
			case ROLLBACKING -> target == VERIFIED
					|| target == FAILED
					|| target == RECOVERING;
			case RECOVERING -> target == NORMAL
					|| target == DEGRADED
					|| target == UNSTABLE
					|| target == CONVERGED
					|| target == FAILED;
			case UNSTABLE -> target == VERIFYING
					|| target == ROLLING_BACK
					|| target == ROLLBACKING
					|| target == RECOVERING
					|| target == FAILED;
			case CONVERGED -> target == DEGRADED
					|| target == FAILED;
			case FAILED -> false;
		};
	}
}
