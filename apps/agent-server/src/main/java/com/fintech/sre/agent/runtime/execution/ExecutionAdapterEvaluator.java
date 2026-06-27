package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionAdapterEvaluator {

	public ExecutionAdapter evaluate(
			ExecutionEngineSelectorIntegrationResult executionEngineSelectorIntegration,
			String adapterIdentifier,
			String adapterType,
			String adapterBinding,
			String adapterPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				executionEngineSelectorIntegration,
				"executionEngineSelectorIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionAdapter(
				level(
						executionEngineSelectorIntegration,
						adapterIdentifier,
						adapterType,
						adapterBinding,
						adapterPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						executionEngineSelectorIntegration,
						adapterIdentifier,
						adapterType,
						adapterBinding,
						adapterPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						executionEngineSelectorIntegration,
						adapterIdentifier,
						adapterType,
						adapterBinding,
						adapterPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				executionEngineSelectorIntegration,
				adapterIdentifier,
				adapterType,
				adapterBinding,
				adapterPolicy,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionAdapterLevel level(
			ExecutionEngineSelectorIntegrationResult executionEngineSelectorIntegration,
			String adapterIdentifier,
			String adapterType,
			String adapterBinding,
			String adapterPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionAdapterLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionAdapterLevel.BLOCKED;
		}
		if (missingText(adapterIdentifier)) {
			return ExecutionAdapterLevel.BLOCKED;
		}
		if (missingText(adapterType)) {
			return ExecutionAdapterLevel.BLOCKED;
		}
		if (missingText(adapterBinding)) {
			return ExecutionAdapterLevel.BLOCKED;
		}
		if (missingText(adapterPolicy)) {
			return ExecutionAdapterLevel.BLOCKED;
		}

		return switch (executionEngineSelectorIntegration.status()) {
			case EXECUTION_ENGINE_SELECTOR_READY_VIEW ->
				ExecutionAdapterLevel.EXECUTION_ADAPTER_READY;
			case PARTIAL_EXECUTION_ENGINE_SELECTOR -> ExecutionAdapterLevel.PARTIAL;
			case NOT_READY -> ExecutionAdapterLevel.NOT_READY;
			case UNRELIABLE -> ExecutionAdapterLevel.UNRELIABLE;
			case BLOCKED -> ExecutionAdapterLevel.BLOCKED;
			case UNKNOWN -> ExecutionAdapterLevel.UNKNOWN;
		};
	}

	private ExecutionAdapterReason reason(
			ExecutionEngineSelectorIntegrationResult executionEngineSelectorIntegration,
			String adapterIdentifier,
			String adapterType,
			String adapterBinding,
			String adapterPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionAdapterReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionAdapterReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(adapterIdentifier)) {
			return ExecutionAdapterReason.MISSING_ADAPTER_IDENTIFIER;
		}
		if (missingText(adapterType)) {
			return ExecutionAdapterReason.MISSING_ADAPTER_TYPE;
		}
		if (missingText(adapterBinding)) {
			return ExecutionAdapterReason.MISSING_ADAPTER_BINDING;
		}
		if (missingText(adapterPolicy)) {
			return ExecutionAdapterReason.MISSING_ADAPTER_POLICY;
		}

		return switch (executionEngineSelectorIntegration.status()) {
			case EXECUTION_ENGINE_SELECTOR_READY_VIEW ->
				ExecutionAdapterReason.EXECUTION_ENGINE_SELECTOR_READY;
			case PARTIAL_EXECUTION_ENGINE_SELECTOR ->
				ExecutionAdapterReason.PARTIAL_EXECUTION_ENGINE_SELECTOR;
			case NOT_READY ->
				ExecutionAdapterReason.NOT_READY_EXECUTION_ENGINE_SELECTOR;
			case UNRELIABLE ->
				ExecutionAdapterReason.UNRELIABLE_EXECUTION_ENGINE_SELECTOR;
			case BLOCKED ->
				ExecutionAdapterReason.BLOCKED_EXECUTION_ENGINE_SELECTOR;
			case UNKNOWN -> ExecutionAdapterReason.UNKNOWN;
		};
	}

	private ExecutionAdapterScope scope(
			ExecutionEngineSelectorIntegrationResult executionEngineSelectorIntegration,
			String adapterIdentifier,
			String adapterType,
			String adapterBinding,
			String adapterPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionAdapterScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionAdapterScope.LIFECYCLE_RISK;
		}
		if (missingText(adapterIdentifier)) {
			return ExecutionAdapterScope.EXECUTION_ADAPTER;
		}
		if (missingText(adapterType)) {
			return ExecutionAdapterScope.ADAPTER_TYPE;
		}
		if (missingText(adapterBinding)) {
			return ExecutionAdapterScope.ADAPTER_BINDING;
		}
		if (missingText(adapterPolicy)) {
			return ExecutionAdapterScope.ADAPTER_POLICY;
		}

		return ExecutionAdapterScope.EXECUTION_ADAPTER;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
