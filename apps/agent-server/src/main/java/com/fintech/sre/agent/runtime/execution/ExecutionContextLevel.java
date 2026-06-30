package com.fintech.sre.agent.runtime.execution;

public enum ExecutionContextLevel {
	EXECUTION_CONTEXT_READY,
	PARTIAL,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
