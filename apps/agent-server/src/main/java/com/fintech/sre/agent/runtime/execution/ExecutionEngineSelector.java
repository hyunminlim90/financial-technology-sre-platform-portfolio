package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ExecutionEngineSelector(
		ExecutionEngineSelectorLevel level,
		ExecutionEngineSelectorReason reason,
		ExecutionEngineSelectorScope scope,
		ExecutionEngineRegistryIntegrationResult executionEngineRegistryIntegration,
		String selectorIdentifier,
		String engineSelectionPolicy,
		String engineCapabilityRequirement,
		boolean selectorGuardrailPresent,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ExecutionEngineSelector {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				executionEngineRegistryIntegration,
				"executionEngineRegistryIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
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

	public boolean kubernetesApiCall() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}
}
