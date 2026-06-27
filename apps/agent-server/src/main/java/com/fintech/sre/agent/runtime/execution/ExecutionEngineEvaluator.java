package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionEngineEvaluator {

	public ExecutionEngine evaluate(
			ExecutionDispatchIntegrationResult executionDispatchIntegration,
			String executionEngineIdentifier,
			String executionEngineType,
			String executionEndpointBinding,
			String executionPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				executionDispatchIntegration,
				"executionDispatchIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionEngine(
				level(
						executionDispatchIntegration,
						executionEngineIdentifier,
						executionEngineType,
						executionEndpointBinding,
						executionPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						executionDispatchIntegration,
						executionEngineIdentifier,
						executionEngineType,
						executionEndpointBinding,
						executionPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						executionDispatchIntegration,
						executionEngineIdentifier,
						executionEngineType,
						executionEndpointBinding,
						executionPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				executionDispatchIntegration,
				executionEngineIdentifier,
				executionEngineType,
				executionEndpointBinding,
				executionPolicy,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionEngineLevel level(
			ExecutionDispatchIntegrationResult executionDispatchIntegration,
			String executionEngineIdentifier,
			String executionEngineType,
			String executionEndpointBinding,
			String executionPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionEngineLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionEngineLevel.BLOCKED;
		}
		if (missingText(executionEngineIdentifier)) {
			return ExecutionEngineLevel.BLOCKED;
		}
		if (missingText(executionEngineType)) {
			return ExecutionEngineLevel.BLOCKED;
		}
		if (missingText(executionEndpointBinding)) {
			return ExecutionEngineLevel.BLOCKED;
		}
		if (missingText(executionPolicy)) {
			return ExecutionEngineLevel.BLOCKED;
		}

		return switch (executionDispatchIntegration.status()) {
			case DISPATCH_READY_VIEW -> ExecutionEngineLevel.EXECUTION_ENGINE_READY;
			case PARTIAL_DISPATCH -> ExecutionEngineLevel.PARTIAL;
			case NOT_READY -> ExecutionEngineLevel.NOT_READY;
			case UNRELIABLE -> ExecutionEngineLevel.UNRELIABLE;
			case BLOCKED -> ExecutionEngineLevel.BLOCKED;
			case UNKNOWN -> ExecutionEngineLevel.UNKNOWN;
		};
	}

	private ExecutionEngineReason reason(
			ExecutionDispatchIntegrationResult executionDispatchIntegration,
			String executionEngineIdentifier,
			String executionEngineType,
			String executionEndpointBinding,
			String executionPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionEngineReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionEngineReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(executionEngineIdentifier)) {
			return ExecutionEngineReason.MISSING_EXECUTION_ENGINE_IDENTIFIER;
		}
		if (missingText(executionEngineType)) {
			return ExecutionEngineReason.MISSING_EXECUTION_ENGINE_TYPE;
		}
		if (missingText(executionEndpointBinding)) {
			return ExecutionEngineReason.MISSING_EXECUTION_ENDPOINT_BINDING;
		}
		if (missingText(executionPolicy)) {
			return ExecutionEngineReason.MISSING_EXECUTION_POLICY;
		}

		return switch (executionDispatchIntegration.status()) {
			case DISPATCH_READY_VIEW -> ExecutionEngineReason.DISPATCH_READY;
			case PARTIAL_DISPATCH -> ExecutionEngineReason.PARTIAL_DISPATCH;
			case NOT_READY -> ExecutionEngineReason.NOT_READY_DISPATCH;
			case UNRELIABLE -> ExecutionEngineReason.UNRELIABLE_DISPATCH;
			case BLOCKED -> ExecutionEngineReason.BLOCKED_DISPATCH;
			case UNKNOWN -> ExecutionEngineReason.UNKNOWN;
		};
	}

	private ExecutionEngineScope scope(
			ExecutionDispatchIntegrationResult executionDispatchIntegration,
			String executionEngineIdentifier,
			String executionEngineType,
			String executionEndpointBinding,
			String executionPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionEngineScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionEngineScope.LIFECYCLE_RISK;
		}
		if (missingText(executionEngineIdentifier)) {
			return ExecutionEngineScope.EXECUTION_ENGINE;
		}
		if (missingText(executionEngineType)) {
			return ExecutionEngineScope.EXECUTION_ENGINE_TYPE;
		}
		if (missingText(executionEndpointBinding)) {
			return ExecutionEngineScope.EXECUTION_ENDPOINT;
		}
		if (missingText(executionPolicy)) {
			return ExecutionEngineScope.EXECUTION_POLICY;
		}

		return ExecutionEngineScope.EXECUTION_ENGINE;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
