package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

public record ExecutionEngineSelectorIntegrationResult(
		ExecutionEngineSelector executionEngineSelector,
		ExecutionEngineSelectorIntegrationStatus status,
		ExecutionEngineSelectorIntegrationReason reason,
		ExecutionEngineSelectorIntegrationScope scope,
		boolean operatorFacingExecutionEngineSelectorVisible,
		boolean executionEngineSelectorCertaintyAllowed
) {
	public ExecutionEngineSelectorIntegrationResult {
		Objects.requireNonNull(
				executionEngineSelector,
				"executionEngineSelector must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualEngineSelection() {
		return false;
	}

	public boolean registryLookup() {
		return false;
	}

	public boolean engineDiscovery() {
		return false;
	}

	public boolean springBeanLookup() {
		return false;
	}

	public boolean serviceLoaderLookup() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
