package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ExecutionEngineRegistry(
		ExecutionEngineRegistryLevel level,
		ExecutionEngineRegistryReason reason,
		ExecutionEngineRegistryScope scope,
		ExecutionEngineIntegrationResult executionEngineIntegration,
		String registryIdentifier,
		boolean engineRegistrationPresent,
		String registryPolicy,
		boolean registryGuardrailPresent,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ExecutionEngineRegistry {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				executionEngineIntegration,
				"executionEngineIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
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
}
