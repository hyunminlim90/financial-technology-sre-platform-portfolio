package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

public record ExecutionAdapterIntegrationResult(
		ExecutionAdapter executionAdapter,
		ExecutionAdapterIntegrationStatus status,
		ExecutionAdapterIntegrationReason reason,
		ExecutionAdapterIntegrationScope scope,
		boolean operatorFacingExecutionAdapterVisible,
		boolean executionAdapterCertaintyAllowed
) {
	public ExecutionAdapterIntegrationResult {
		Objects.requireNonNull(
				executionAdapter,
				"executionAdapter must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualAdapterImplementation() {
		return false;
	}

	public boolean adapterInvocation() {
		return false;
	}

	public boolean kubernetesAdapter() {
		return false;
	}

	public boolean argoCdAdapter() {
		return false;
	}

	public boolean terraformOrOpenTofuAdapter() {
		return false;
	}

	public boolean sshOrAnsibleAdapter() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
