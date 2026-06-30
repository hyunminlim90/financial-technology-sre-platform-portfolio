package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

public record ExecutionContextIntegrationResult(
		ExecutionContext executionContext,
		ExecutionContextIntegrationStatus status,
		ExecutionContextIntegrationReason reason,
		ExecutionContextIntegrationScope scope,
		boolean operatorFacingExecutionContextVisible,
		boolean executionContextCertaintyAllowed
) {
	public ExecutionContextIntegrationResult {
		Objects.requireNonNull(
				executionContext,
				"executionContext must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualContextCreation() {
		return false;
	}

	public boolean threadLocalCreation() {
		return false;
	}

	public boolean securityContextCreation() {
		return false;
	}

	public boolean transactionContextCreation() {
		return false;
	}

	public boolean kubernetesContextCreation() {
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
