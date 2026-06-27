package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionEngineSelectorEvaluator {

	public ExecutionEngineSelector evaluate(
			ExecutionEngineRegistryIntegrationResult executionEngineRegistryIntegration,
			String selectorIdentifier,
			String engineSelectionPolicy,
			String engineCapabilityRequirement,
			boolean selectorGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				executionEngineRegistryIntegration,
				"executionEngineRegistryIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionEngineSelector(
				level(
						executionEngineRegistryIntegration,
						selectorIdentifier,
						engineSelectionPolicy,
						engineCapabilityRequirement,
						selectorGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						executionEngineRegistryIntegration,
						selectorIdentifier,
						engineSelectionPolicy,
						engineCapabilityRequirement,
						selectorGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						executionEngineRegistryIntegration,
						selectorIdentifier,
						engineSelectionPolicy,
						engineCapabilityRequirement,
						selectorGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				executionEngineRegistryIntegration,
				selectorIdentifier,
				engineSelectionPolicy,
				engineCapabilityRequirement,
				selectorGuardrailPresent,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionEngineSelectorLevel level(
			ExecutionEngineRegistryIntegrationResult executionEngineRegistryIntegration,
			String selectorIdentifier,
			String engineSelectionPolicy,
			String engineCapabilityRequirement,
			boolean selectorGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionEngineSelectorLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionEngineSelectorLevel.BLOCKED;
		}
		if (missingText(selectorIdentifier)) {
			return ExecutionEngineSelectorLevel.BLOCKED;
		}
		if (missingText(engineSelectionPolicy)) {
			return ExecutionEngineSelectorLevel.BLOCKED;
		}
		if (missingText(engineCapabilityRequirement)) {
			return ExecutionEngineSelectorLevel.BLOCKED;
		}
		if (!selectorGuardrailPresent) {
			return ExecutionEngineSelectorLevel.BLOCKED;
		}

		return switch (executionEngineRegistryIntegration.status()) {
			case EXECUTION_ENGINE_REGISTRY_READY_VIEW ->
				ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY;
			case PARTIAL_EXECUTION_ENGINE_REGISTRY -> ExecutionEngineSelectorLevel.PARTIAL;
			case NOT_READY -> ExecutionEngineSelectorLevel.NOT_READY;
			case UNRELIABLE -> ExecutionEngineSelectorLevel.UNRELIABLE;
			case BLOCKED -> ExecutionEngineSelectorLevel.BLOCKED;
			case UNKNOWN -> ExecutionEngineSelectorLevel.UNKNOWN;
		};
	}

	private ExecutionEngineSelectorReason reason(
			ExecutionEngineRegistryIntegrationResult executionEngineRegistryIntegration,
			String selectorIdentifier,
			String engineSelectionPolicy,
			String engineCapabilityRequirement,
			boolean selectorGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionEngineSelectorReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionEngineSelectorReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(selectorIdentifier)) {
			return ExecutionEngineSelectorReason.MISSING_SELECTOR_IDENTIFIER;
		}
		if (missingText(engineSelectionPolicy)) {
			return ExecutionEngineSelectorReason.MISSING_ENGINE_SELECTION_POLICY;
		}
		if (missingText(engineCapabilityRequirement)) {
			return ExecutionEngineSelectorReason.MISSING_ENGINE_CAPABILITY_REQUIREMENT;
		}
		if (!selectorGuardrailPresent) {
			return ExecutionEngineSelectorReason.MISSING_SELECTOR_GUARDRAIL;
		}

		return switch (executionEngineRegistryIntegration.status()) {
			case EXECUTION_ENGINE_REGISTRY_READY_VIEW ->
				ExecutionEngineSelectorReason.EXECUTION_ENGINE_REGISTRY_READY;
			case PARTIAL_EXECUTION_ENGINE_REGISTRY ->
				ExecutionEngineSelectorReason.PARTIAL_EXECUTION_ENGINE_REGISTRY;
			case NOT_READY ->
				ExecutionEngineSelectorReason.NOT_READY_EXECUTION_ENGINE_REGISTRY;
			case UNRELIABLE ->
				ExecutionEngineSelectorReason.UNRELIABLE_EXECUTION_ENGINE_REGISTRY;
			case BLOCKED ->
				ExecutionEngineSelectorReason.BLOCKED_EXECUTION_ENGINE_REGISTRY;
			case UNKNOWN -> ExecutionEngineSelectorReason.UNKNOWN;
		};
	}

	private ExecutionEngineSelectorScope scope(
			ExecutionEngineRegistryIntegrationResult executionEngineRegistryIntegration,
			String selectorIdentifier,
			String engineSelectionPolicy,
			String engineCapabilityRequirement,
			boolean selectorGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionEngineSelectorScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionEngineSelectorScope.LIFECYCLE_RISK;
		}
		if (missingText(selectorIdentifier)) {
			return ExecutionEngineSelectorScope.EXECUTION_ENGINE_SELECTOR;
		}
		if (missingText(engineSelectionPolicy)) {
			return ExecutionEngineSelectorScope.ENGINE_SELECTION_POLICY;
		}
		if (missingText(engineCapabilityRequirement)) {
			return ExecutionEngineSelectorScope.ENGINE_CAPABILITY_REQUIREMENT;
		}
		if (!selectorGuardrailPresent) {
			return ExecutionEngineSelectorScope.SELECTOR_GUARDRAIL;
		}

		return ExecutionEngineSelectorScope.EXECUTION_ENGINE_SELECTOR;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
