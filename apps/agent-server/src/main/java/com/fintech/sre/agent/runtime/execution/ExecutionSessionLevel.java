package com.fintech.sre.agent.runtime.execution;

public enum ExecutionSessionLevel {
	EXECUTION_SESSION_READY,
	PARTIAL,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
