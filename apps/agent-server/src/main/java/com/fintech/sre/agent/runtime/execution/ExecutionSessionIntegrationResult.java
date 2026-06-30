package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

public record ExecutionSessionIntegrationResult(
		ExecutionSession executionSession,
		ExecutionSessionIntegrationStatus status,
		ExecutionSessionIntegrationReason reason,
		ExecutionSessionIntegrationScope scope,
		boolean operatorFacingExecutionSessionVisible,
		boolean executionSessionCertaintyAllowed
) {
	public ExecutionSessionIntegrationResult {
		Objects.requireNonNull(
				executionSession,
				"executionSession must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualSessionCreation() {
		return false;
	}

	public boolean threadCreation() {
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

	public boolean workflowExecution() {
		return false;
	}

	public boolean runtimeExecution() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
