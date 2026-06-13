package com.fintech.sre.agent.runtime.readiness;

public enum ActionAdmissionReadinessLevel {
	READY,
	PARTIAL,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
