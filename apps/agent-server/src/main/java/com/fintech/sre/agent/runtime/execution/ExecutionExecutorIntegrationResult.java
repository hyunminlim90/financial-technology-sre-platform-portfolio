package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

public record ExecutionExecutorIntegrationResult(
		ExecutionExecutor executionExecutor,
		ExecutionExecutorIntegrationStatus status,
		ExecutionExecutorIntegrationReason reason,
		ExecutionExecutorIntegrationScope scope,
		boolean operatorFacingExecutionExecutorVisible,
		boolean executionExecutorCertaintyAllowed
) {
	public ExecutionExecutorIntegrationResult {
		Objects.requireNonNull(
				executionExecutor,
				"executionExecutor must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean executorImplementation() {
		return false;
	}

	public boolean executorThreadCreation() {
		return false;
	}

	public boolean adapterInvocation() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean kubernetesOrArgoOrTerraformOrSshOrAnsibleCall() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
