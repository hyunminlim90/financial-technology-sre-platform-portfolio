package com.fintech.sre.agent.runtime.reliability;

public record ReliabilityScore(int value) {
	public ReliabilityScore {
		if (value < 0 || value > 100) {
			throw new IllegalArgumentException(
					"reliability score must be between 0 and 100"
			);
		}
	}

	public boolean degraded() {
		return value < 80;
	}

	public boolean critical() {
		return value < 50;
	}
}
