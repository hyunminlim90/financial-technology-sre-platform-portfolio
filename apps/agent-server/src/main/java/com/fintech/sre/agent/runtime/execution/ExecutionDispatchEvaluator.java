package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionDispatchEvaluator {

	public ExecutionDispatch evaluate(
			ExecutionPlanIntegrationResult executionPlanIntegration,
			String dispatchIdentifier,
			String executionEndpoint,
			String dispatchPolicy,
			boolean dispatchGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				executionPlanIntegration,
				"executionPlanIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionDispatch(
				level(
						executionPlanIntegration,
						dispatchIdentifier,
						executionEndpoint,
						dispatchPolicy,
						dispatchGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						executionPlanIntegration,
						dispatchIdentifier,
						executionEndpoint,
						dispatchPolicy,
						dispatchGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						executionPlanIntegration,
						dispatchIdentifier,
						executionEndpoint,
						dispatchPolicy,
						dispatchGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				executionPlanIntegration,
				dispatchIdentifier,
				executionEndpoint,
				dispatchPolicy,
				dispatchGuardrailPresent,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionDispatchLevel level(
			ExecutionPlanIntegrationResult executionPlanIntegration,
			String dispatchIdentifier,
			String executionEndpoint,
			String dispatchPolicy,
			boolean dispatchGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionDispatchLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionDispatchLevel.BLOCKED;
		}
		if (missingText(dispatchIdentifier)) {
			return ExecutionDispatchLevel.BLOCKED;
		}
		if (missingText(executionEndpoint)) {
			return ExecutionDispatchLevel.BLOCKED;
		}
		if (missingText(dispatchPolicy)) {
			return ExecutionDispatchLevel.BLOCKED;
		}
		if (!dispatchGuardrailPresent) {
			return ExecutionDispatchLevel.BLOCKED;
		}

		return switch (executionPlanIntegration.status()) {
			case EXECUTION_PLAN_READY_VIEW -> ExecutionDispatchLevel.DISPATCH_READY;
			case PARTIAL_EXECUTION_PLAN -> ExecutionDispatchLevel.PARTIAL;
			case NOT_READY -> ExecutionDispatchLevel.NOT_READY;
			case UNRELIABLE -> ExecutionDispatchLevel.UNRELIABLE;
			case BLOCKED -> ExecutionDispatchLevel.BLOCKED;
			case UNKNOWN -> ExecutionDispatchLevel.UNKNOWN;
		};
	}

	private ExecutionDispatchReason reason(
			ExecutionPlanIntegrationResult executionPlanIntegration,
			String dispatchIdentifier,
			String executionEndpoint,
			String dispatchPolicy,
			boolean dispatchGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionDispatchReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionDispatchReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(dispatchIdentifier)) {
			return ExecutionDispatchReason.MISSING_DISPATCH_IDENTIFIER;
		}
		if (missingText(executionEndpoint)) {
			return ExecutionDispatchReason.MISSING_EXECUTION_ENDPOINT;
		}
		if (missingText(dispatchPolicy)) {
			return ExecutionDispatchReason.MISSING_DISPATCH_POLICY;
		}
		if (!dispatchGuardrailPresent) {
			return ExecutionDispatchReason.MISSING_DISPATCH_GUARDRAIL;
		}

		return switch (executionPlanIntegration.status()) {
			case EXECUTION_PLAN_READY_VIEW -> ExecutionDispatchReason.EXECUTION_PLAN_READY;
			case PARTIAL_EXECUTION_PLAN -> ExecutionDispatchReason.PARTIAL_EXECUTION_PLAN;
			case NOT_READY -> ExecutionDispatchReason.NOT_READY_EXECUTION_PLAN;
			case UNRELIABLE -> ExecutionDispatchReason.UNRELIABLE_EXECUTION_PLAN;
			case BLOCKED -> ExecutionDispatchReason.BLOCKED_EXECUTION_PLAN;
			case UNKNOWN -> ExecutionDispatchReason.UNKNOWN;
		};
	}

	private ExecutionDispatchScope scope(
			ExecutionPlanIntegrationResult executionPlanIntegration,
			String dispatchIdentifier,
			String executionEndpoint,
			String dispatchPolicy,
			boolean dispatchGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionDispatchScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionDispatchScope.LIFECYCLE_RISK;
		}
		if (missingText(dispatchIdentifier)) {
			return ExecutionDispatchScope.EXECUTION_DISPATCH;
		}
		if (missingText(executionEndpoint)) {
			return ExecutionDispatchScope.EXECUTION_ENDPOINT;
		}
		if (missingText(dispatchPolicy)) {
			return ExecutionDispatchScope.DISPATCH_POLICY;
		}
		if (!dispatchGuardrailPresent) {
			return ExecutionDispatchScope.DISPATCH_GUARDRAIL;
		}

		return ExecutionDispatchScope.EXECUTION_DISPATCH;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
