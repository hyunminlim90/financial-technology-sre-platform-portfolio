package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ExecutionExecutor(
		ExecutionExecutorLevel level,
		ExecutionExecutorReason reason,
		ExecutionExecutorScope scope,
		ExecutionAdapterIntegrationResult executionAdapterIntegration,
		String executorIdentifier,
		String executionStrategy,
		String executionBoundary,
		String executorPolicy,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ExecutionExecutor {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				executionAdapterIntegration,
				"executionAdapterIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
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

	public boolean kubernetesApiCall() {
		return false;
	}

	public boolean kubectlExecution() {
		return false;
	}

	public boolean argoCdSync() {
		return false;
	}

	public boolean terraformOrOpenTofuApply() {
		return false;
	}

	public boolean sshExecution() {
		return false;
	}

	public boolean ansibleExecution() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean executionEngineExecution() {
		return false;
	}
}
