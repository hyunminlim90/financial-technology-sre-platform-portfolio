package com.fintech.sre.agent.runtime.reliability;

public enum VerificationResult {
	PENDING,
	CONFIRMED,
	INCONCLUSIVE,
	REGRESSION_DETECTED;

	public boolean terminal() {
		return this != PENDING;
	}
}
