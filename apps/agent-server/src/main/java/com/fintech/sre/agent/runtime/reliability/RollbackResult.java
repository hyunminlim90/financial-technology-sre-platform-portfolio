package com.fintech.sre.agent.runtime.reliability;

public enum RollbackResult {
	NOT_ATTEMPTED,
	PREPARED,
	APPLIED,
	FAILED;

	public boolean failed() {
		return this == FAILED;
	}
}
