package com.fintech.sre.agent.runtime.execution;

public enum ExecutionExecutorLevel {
	EXECUTION_EXECUTOR_READY,
	PARTIAL,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
