package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

public record ExecutionEngineRegistryIntegrationResult(
		ExecutionEngineRegistry executionEngineRegistry,
		ExecutionEngineRegistryIntegrationStatus status,
		ExecutionEngineRegistryIntegrationReason reason,
		ExecutionEngineRegistryIntegrationScope scope,
		boolean operatorFacingExecutionEngineRegistryVisible,
		boolean executionEngineRegistryCertaintyAllowed
) {
	public ExecutionEngineRegistryIntegrationResult {
		Objects.requireNonNull(
				executionEngineRegistry,
				"executionEngineRegistry must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean registryImplementation() {
		return false;
	}

	public boolean engineDiscovery() {
		return false;
	}

	public boolean springBeanRegistry() {
		return false;
	}

	public boolean serviceLoader() {
		return false;
	}

	public boolean actualExecutionEngineSelection() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
