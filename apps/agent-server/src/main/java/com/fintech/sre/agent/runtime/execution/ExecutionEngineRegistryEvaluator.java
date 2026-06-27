package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionEngineRegistryEvaluator {

	public ExecutionEngineRegistry evaluate(
			ExecutionEngineIntegrationResult executionEngineIntegration,
			String registryIdentifier,
			boolean engineRegistrationPresent,
			String registryPolicy,
			boolean registryGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				executionEngineIntegration,
				"executionEngineIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionEngineRegistry(
				level(
						executionEngineIntegration,
						registryIdentifier,
						engineRegistrationPresent,
						registryPolicy,
						registryGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						executionEngineIntegration,
						registryIdentifier,
						engineRegistrationPresent,
						registryPolicy,
						registryGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						executionEngineIntegration,
						registryIdentifier,
						engineRegistrationPresent,
						registryPolicy,
						registryGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				executionEngineIntegration,
				registryIdentifier,
				engineRegistrationPresent,
				registryPolicy,
				registryGuardrailPresent,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionEngineRegistryLevel level(
			ExecutionEngineIntegrationResult executionEngineIntegration,
			String registryIdentifier,
			boolean engineRegistrationPresent,
			String registryPolicy,
			boolean registryGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionEngineRegistryLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionEngineRegistryLevel.BLOCKED;
		}
		if (missingText(registryIdentifier)) {
			return ExecutionEngineRegistryLevel.BLOCKED;
		}
		if (!engineRegistrationPresent) {
			return ExecutionEngineRegistryLevel.BLOCKED;
		}
		if (missingText(registryPolicy)) {
			return ExecutionEngineRegistryLevel.BLOCKED;
		}
		if (!registryGuardrailPresent) {
			return ExecutionEngineRegistryLevel.BLOCKED;
		}

		return switch (executionEngineIntegration.status()) {
			case EXECUTION_ENGINE_READY_VIEW ->
				ExecutionEngineRegistryLevel.EXECUTION_ENGINE_REGISTRY_READY;
			case PARTIAL_EXECUTION_ENGINE -> ExecutionEngineRegistryLevel.PARTIAL;
			case NOT_READY -> ExecutionEngineRegistryLevel.NOT_READY;
			case UNRELIABLE -> ExecutionEngineRegistryLevel.UNRELIABLE;
			case BLOCKED -> ExecutionEngineRegistryLevel.BLOCKED;
			case UNKNOWN -> ExecutionEngineRegistryLevel.UNKNOWN;
		};
	}

	private ExecutionEngineRegistryReason reason(
			ExecutionEngineIntegrationResult executionEngineIntegration,
			String registryIdentifier,
			boolean engineRegistrationPresent,
			String registryPolicy,
			boolean registryGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionEngineRegistryReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionEngineRegistryReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(registryIdentifier)) {
			return ExecutionEngineRegistryReason.MISSING_REGISTRY_IDENTIFIER;
		}
		if (!engineRegistrationPresent) {
			return ExecutionEngineRegistryReason.MISSING_ENGINE_REGISTRATION;
		}
		if (missingText(registryPolicy)) {
			return ExecutionEngineRegistryReason.MISSING_REGISTRY_POLICY;
		}
		if (!registryGuardrailPresent) {
			return ExecutionEngineRegistryReason.MISSING_REGISTRY_GUARDRAIL;
		}

		return switch (executionEngineIntegration.status()) {
			case EXECUTION_ENGINE_READY_VIEW ->
				ExecutionEngineRegistryReason.EXECUTION_ENGINE_READY;
			case PARTIAL_EXECUTION_ENGINE ->
				ExecutionEngineRegistryReason.PARTIAL_EXECUTION_ENGINE;
			case NOT_READY -> ExecutionEngineRegistryReason.NOT_READY_EXECUTION_ENGINE;
			case UNRELIABLE -> ExecutionEngineRegistryReason.UNRELIABLE_EXECUTION_ENGINE;
			case BLOCKED -> ExecutionEngineRegistryReason.BLOCKED_EXECUTION_ENGINE;
			case UNKNOWN -> ExecutionEngineRegistryReason.UNKNOWN;
		};
	}

	private ExecutionEngineRegistryScope scope(
			ExecutionEngineIntegrationResult executionEngineIntegration,
			String registryIdentifier,
			boolean engineRegistrationPresent,
			String registryPolicy,
			boolean registryGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionEngineRegistryScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionEngineRegistryScope.LIFECYCLE_RISK;
		}
		if (missingText(registryIdentifier)) {
			return ExecutionEngineRegistryScope.EXECUTION_ENGINE_REGISTRY;
		}
		if (!engineRegistrationPresent) {
			return ExecutionEngineRegistryScope.ENGINE_REGISTRATION;
		}
		if (missingText(registryPolicy)) {
			return ExecutionEngineRegistryScope.REGISTRY_POLICY;
		}
		if (!registryGuardrailPresent) {
			return ExecutionEngineRegistryScope.REGISTRY_GUARDRAIL;
		}

		return ExecutionEngineRegistryScope.EXECUTION_ENGINE_REGISTRY;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
