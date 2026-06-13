package com.fintech.sre.agent.runtime.readiness;

public enum VerificationReadinessLevel {
	READY,
	PARTIAL,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
