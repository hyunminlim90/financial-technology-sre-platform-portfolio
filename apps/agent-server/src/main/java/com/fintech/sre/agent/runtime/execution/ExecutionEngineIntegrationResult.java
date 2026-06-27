package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

public record ExecutionEngineIntegrationResult(
		ExecutionEngine executionEngine,
		ExecutionEngineIntegrationStatus status,
		ExecutionEngineIntegrationReason reason,
		ExecutionEngineIntegrationScope scope,
		boolean operatorFacingExecutionEngineVisible,
		boolean executionEngineCertaintyAllowed
) {
	public ExecutionEngineIntegrationResult {
		Objects.requireNonNull(executionEngine, "executionEngine must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean dispatchPerformed() {
		return false;
	}

	public boolean kubernetesApiCall() {
		return false;
	}

	public boolean kubectlExecution() {
		return false;
	}

	public boolean argoCdSync() {
		return false;
	}

	public boolean terraformApply() {
		return false;
	}

	public boolean sshOrAnsibleExecution() {
		return false;
	}

	public boolean specificExecutionEngineImplementation() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
