package com.fintech.sre.agent.runtime.reliability;

public enum PropagationSignal {
	LOCALIZED,
	CROSS_SERVICE,
	CROSS_CLUSTER,
	UNKNOWN;

	public boolean widespread() {
		return this == CROSS_SERVICE || this == CROSS_CLUSTER;
	}
}
