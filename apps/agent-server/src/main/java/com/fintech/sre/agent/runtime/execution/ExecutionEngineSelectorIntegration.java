package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionEngineSelectorIntegration {

	public ExecutionEngineSelectorIntegrationResult integrate(
			ExecutionEngineSelector executionEngineSelector
	) {
		if (executionEngineSelector == null) {
			throw new NullPointerException(
					"executionEngineSelector must not be null"
			);
		}

		if (executionEngineSelector.paymentSafetyUncertainty()) {
			return result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.BLOCKED,
					ExecutionEngineSelectorIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionEngineSelectorIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionEngineSelector.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.BLOCKED,
					ExecutionEngineSelectorIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionEngineSelectorIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingSelectorIdentifier(executionEngineSelector)) {
			return result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.BLOCKED,
					ExecutionEngineSelectorIntegrationReason.MISSING_SELECTOR_IDENTIFIER,
					ExecutionEngineSelectorIntegrationScope.EXECUTION_ENGINE_SELECTOR,
					false,
					false
			);
		}
		if (missingEngineSelectionPolicy(executionEngineSelector)) {
			return result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.BLOCKED,
					ExecutionEngineSelectorIntegrationReason.MISSING_ENGINE_SELECTION_POLICY,
					ExecutionEngineSelectorIntegrationScope.ENGINE_SELECTION_POLICY,
					false,
					false
			);
		}
		if (missingEngineCapabilityRequirement(executionEngineSelector)) {
			return result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.BLOCKED,
					ExecutionEngineSelectorIntegrationReason.MISSING_ENGINE_CAPABILITY_REQUIREMENT,
					ExecutionEngineSelectorIntegrationScope.ENGINE_CAPABILITY_REQUIREMENT,
					false,
					false
			);
		}
		if (!executionEngineSelector.selectorGuardrailPresent()) {
			return result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.BLOCKED,
					ExecutionEngineSelectorIntegrationReason.MISSING_SELECTOR_GUARDRAIL,
					ExecutionEngineSelectorIntegrationScope.SELECTOR_GUARDRAIL,
					false,
					false
			);
		}

		return switch (executionEngineSelector.level()) {
			case EXECUTION_ENGINE_SELECTOR_READY -> result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.EXECUTION_ENGINE_SELECTOR_READY_VIEW,
					ExecutionEngineSelectorIntegrationReason.EXECUTION_ENGINE_SELECTOR_READY,
					ExecutionEngineSelectorIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.PARTIAL_EXECUTION_ENGINE_SELECTOR,
					ExecutionEngineSelectorIntegrationReason.PARTIAL_EXECUTION_ENGINE_SELECTOR,
					ExecutionEngineSelectorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.NOT_READY,
					ExecutionEngineSelectorIntegrationReason.NOT_READY_EXECUTION_ENGINE_SELECTOR,
					ExecutionEngineSelectorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.UNRELIABLE,
					ExecutionEngineSelectorIntegrationReason.UNRELIABLE_EXECUTION_ENGINE_SELECTOR,
					ExecutionEngineSelectorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.BLOCKED,
					ExecutionEngineSelectorIntegrationReason.BLOCKED_EXECUTION_ENGINE_SELECTOR,
					ExecutionEngineSelectorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionEngineSelector,
					ExecutionEngineSelectorIntegrationStatus.UNKNOWN,
					ExecutionEngineSelectorIntegrationReason.UNKNOWN,
					ExecutionEngineSelectorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
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

	private boolean missingSelectorIdentifier(
			ExecutionEngineSelector executionEngineSelector
	) {
		return executionEngineSelector.selectorIdentifier() == null
				|| executionEngineSelector.selectorIdentifier().isBlank();
	}

	private boolean missingEngineSelectionPolicy(
			ExecutionEngineSelector executionEngineSelector
	) {
		return executionEngineSelector.engineSelectionPolicy() == null
				|| executionEngineSelector.engineSelectionPolicy().isBlank();
	}

	private boolean missingEngineCapabilityRequirement(
			ExecutionEngineSelector executionEngineSelector
	) {
		return executionEngineSelector.engineCapabilityRequirement() == null
				|| executionEngineSelector.engineCapabilityRequirement().isBlank();
	}

	private ExecutionEngineSelectorIntegrationResult result(
			ExecutionEngineSelector executionEngineSelector,
			ExecutionEngineSelectorIntegrationStatus status,
			ExecutionEngineSelectorIntegrationReason reason,
			ExecutionEngineSelectorIntegrationScope scope,
			boolean operatorFacingExecutionEngineSelectorVisible,
			boolean executionEngineSelectorCertaintyAllowed
	) {
		return new ExecutionEngineSelectorIntegrationResult(
				executionEngineSelector,
				status,
				reason,
				scope,
				operatorFacingExecutionEngineSelectorVisible,
				executionEngineSelectorCertaintyAllowed
		);
	}
}
