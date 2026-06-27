package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionEngineRegistryIntegration {

	public ExecutionEngineRegistryIntegrationResult integrate(
			ExecutionEngineRegistry executionEngineRegistry
	) {
		if (executionEngineRegistry == null) {
			throw new NullPointerException(
					"executionEngineRegistry must not be null"
			);
		}

		if (executionEngineRegistry.paymentSafetyUncertainty()) {
			return result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.BLOCKED,
					ExecutionEngineRegistryIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionEngineRegistryIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionEngineRegistry.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.BLOCKED,
					ExecutionEngineRegistryIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionEngineRegistryIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingRegistryIdentifier(executionEngineRegistry)) {
			return result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.BLOCKED,
					ExecutionEngineRegistryIntegrationReason.MISSING_REGISTRY_IDENTIFIER,
					ExecutionEngineRegistryIntegrationScope.EXECUTION_ENGINE_REGISTRY,
					false,
					false
			);
		}
		if (!executionEngineRegistry.engineRegistrationPresent()) {
			return result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.BLOCKED,
					ExecutionEngineRegistryIntegrationReason.MISSING_ENGINE_REGISTRATION,
					ExecutionEngineRegistryIntegrationScope.ENGINE_REGISTRATION,
					false,
					false
			);
		}
		if (missingRegistryPolicy(executionEngineRegistry)) {
			return result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.BLOCKED,
					ExecutionEngineRegistryIntegrationReason.MISSING_REGISTRY_POLICY,
					ExecutionEngineRegistryIntegrationScope.REGISTRY_POLICY,
					false,
					false
			);
		}
		if (!executionEngineRegistry.registryGuardrailPresent()) {
			return result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.BLOCKED,
					ExecutionEngineRegistryIntegrationReason.MISSING_REGISTRY_GUARDRAIL,
					ExecutionEngineRegistryIntegrationScope.REGISTRY_GUARDRAIL,
					false,
					false
			);
		}

		return switch (executionEngineRegistry.level()) {
			case EXECUTION_ENGINE_REGISTRY_READY -> result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.EXECUTION_ENGINE_REGISTRY_READY_VIEW,
					ExecutionEngineRegistryIntegrationReason.EXECUTION_ENGINE_REGISTRY_READY,
					ExecutionEngineRegistryIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.PARTIAL_EXECUTION_ENGINE_REGISTRY,
					ExecutionEngineRegistryIntegrationReason.PARTIAL_EXECUTION_ENGINE_REGISTRY,
					ExecutionEngineRegistryIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.NOT_READY,
					ExecutionEngineRegistryIntegrationReason.NOT_READY_EXECUTION_ENGINE_REGISTRY,
					ExecutionEngineRegistryIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.UNRELIABLE,
					ExecutionEngineRegistryIntegrationReason.UNRELIABLE_EXECUTION_ENGINE_REGISTRY,
					ExecutionEngineRegistryIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.BLOCKED,
					ExecutionEngineRegistryIntegrationReason.BLOCKED_EXECUTION_ENGINE_REGISTRY,
					ExecutionEngineRegistryIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionEngineRegistry,
					ExecutionEngineRegistryIntegrationStatus.UNKNOWN,
					ExecutionEngineRegistryIntegrationReason.UNKNOWN,
					ExecutionEngineRegistryIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
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

	private boolean missingRegistryIdentifier(
			ExecutionEngineRegistry executionEngineRegistry
	) {
		return executionEngineRegistry.registryIdentifier() == null
				|| executionEngineRegistry.registryIdentifier().isBlank();
	}

	private boolean missingRegistryPolicy(
			ExecutionEngineRegistry executionEngineRegistry
	) {
		return executionEngineRegistry.registryPolicy() == null
				|| executionEngineRegistry.registryPolicy().isBlank();
	}

	private ExecutionEngineRegistryIntegrationResult result(
			ExecutionEngineRegistry executionEngineRegistry,
			ExecutionEngineRegistryIntegrationStatus status,
			ExecutionEngineRegistryIntegrationReason reason,
			ExecutionEngineRegistryIntegrationScope scope,
			boolean operatorFacingExecutionEngineRegistryVisible,
			boolean executionEngineRegistryCertaintyAllowed
	) {
		return new ExecutionEngineRegistryIntegrationResult(
				executionEngineRegistry,
				status,
				reason,
				scope,
				operatorFacingExecutionEngineRegistryVisible,
				executionEngineRegistryCertaintyAllowed
		);
	}
}
