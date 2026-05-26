package com.fintech.sre.agent.runtime.reliability;

public enum ConvergenceStatus {
	CANDIDATE,
	CONVERGED,
	DEGRADED_AFTER_CONVERGENCE,
	REJECTED;

	public boolean terminalSemantic() {
		return this == CONVERGED;
	}
}
