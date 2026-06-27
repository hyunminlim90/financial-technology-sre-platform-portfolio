package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ExecutionSession(
		ExecutionSessionLevel level,
		ExecutionSessionReason reason,
		ExecutionSessionScope scope,
		ExecutionExecutorIntegrationResult executionExecutorIntegration,
		String sessionIdentifier,
		String executionCorrelationIdentifier,
		String executionScope,
		String sessionPolicy,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ExecutionSession {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				executionExecutorIntegration,
				"executionExecutorIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualSessionCreation() {
		return false;
	}

	public boolean executorThreadCreation() {
		return false;
	}

	public boolean transactionStart() {
		return false;
	}

	public boolean kubernetesJobCreation() {
		return false;
	}

	public boolean podCreation() {
		return false;
	}

	public boolean workflowCreation() {
		return false;
	}

	public boolean runtimeExecution() {
		return false;
	}

	public boolean adapterInvocation() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}
}
