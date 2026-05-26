package com.fintech.sre.agent.runtime.reliability;

public enum OperationalUncertainty {
	LOW,
	MODERATE,
	HIGH,
	CRITICAL;

	public boolean requiresHumanEscalation() {
		return this == HIGH || this == CRITICAL;
	}
}
