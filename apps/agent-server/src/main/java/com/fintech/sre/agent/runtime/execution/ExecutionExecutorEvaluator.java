package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionExecutorEvaluator {

	public ExecutionExecutor evaluate(
			ExecutionAdapterIntegrationResult executionAdapterIntegration,
			String executorIdentifier,
			String executionStrategy,
			String executionBoundary,
			String executorPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				executionAdapterIntegration,
				"executionAdapterIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionExecutor(
				level(
						executionAdapterIntegration,
						executorIdentifier,
						executionStrategy,
						executionBoundary,
						executorPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						executionAdapterIntegration,
						executorIdentifier,
						executionStrategy,
						executionBoundary,
						executorPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						executionAdapterIntegration,
						executorIdentifier,
						executionStrategy,
						executionBoundary,
						executorPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				executionAdapterIntegration,
				executorIdentifier,
				executionStrategy,
				executionBoundary,
				executorPolicy,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionExecutorLevel level(
			ExecutionAdapterIntegrationResult executionAdapterIntegration,
			String executorIdentifier,
			String executionStrategy,
			String executionBoundary,
			String executorPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionExecutorLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionExecutorLevel.BLOCKED;
		}
		if (missingText(executorIdentifier)) {
			return ExecutionExecutorLevel.BLOCKED;
		}
		if (missingText(executionStrategy)) {
			return ExecutionExecutorLevel.BLOCKED;
		}
		if (missingText(executionBoundary)) {
			return ExecutionExecutorLevel.BLOCKED;
		}
		if (missingText(executorPolicy)) {
			return ExecutionExecutorLevel.BLOCKED;
		}

		return switch (executionAdapterIntegration.status()) {
			case EXECUTION_ADAPTER_READY_VIEW ->
				ExecutionExecutorLevel.EXECUTION_EXECUTOR_READY;
			case PARTIAL_EXECUTION_ADAPTER -> ExecutionExecutorLevel.PARTIAL;
			case NOT_READY -> ExecutionExecutorLevel.NOT_READY;
			case UNRELIABLE -> ExecutionExecutorLevel.UNRELIABLE;
			case BLOCKED -> ExecutionExecutorLevel.BLOCKED;
			case UNKNOWN -> ExecutionExecutorLevel.UNKNOWN;
		};
	}

	private ExecutionExecutorReason reason(
			ExecutionAdapterIntegrationResult executionAdapterIntegration,
			String executorIdentifier,
			String executionStrategy,
			String executionBoundary,
			String executorPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionExecutorReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionExecutorReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(executorIdentifier)) {
			return ExecutionExecutorReason.MISSING_EXECUTOR_IDENTIFIER;
		}
		if (missingText(executionStrategy)) {
			return ExecutionExecutorReason.MISSING_EXECUTION_STRATEGY;
		}
		if (missingText(executionBoundary)) {
			return ExecutionExecutorReason.MISSING_EXECUTION_BOUNDARY;
		}
		if (missingText(executorPolicy)) {
			return ExecutionExecutorReason.MISSING_EXECUTOR_POLICY;
		}

		return switch (executionAdapterIntegration.status()) {
			case EXECUTION_ADAPTER_READY_VIEW ->
				ExecutionExecutorReason.EXECUTION_ADAPTER_READY;
			case PARTIAL_EXECUTION_ADAPTER ->
				ExecutionExecutorReason.PARTIAL_EXECUTION_ADAPTER;
			case NOT_READY ->
				ExecutionExecutorReason.NOT_READY_EXECUTION_ADAPTER;
			case UNRELIABLE ->
				ExecutionExecutorReason.UNRELIABLE_EXECUTION_ADAPTER;
			case BLOCKED ->
				ExecutionExecutorReason.BLOCKED_EXECUTION_ADAPTER;
			case UNKNOWN -> ExecutionExecutorReason.UNKNOWN;
		};
	}

	private ExecutionExecutorScope scope(
			ExecutionAdapterIntegrationResult executionAdapterIntegration,
			String executorIdentifier,
			String executionStrategy,
			String executionBoundary,
			String executorPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionExecutorScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionExecutorScope.LIFECYCLE_RISK;
		}
		if (missingText(executorIdentifier)) {
			return ExecutionExecutorScope.EXECUTION_EXECUTOR;
		}
		if (missingText(executionStrategy)) {
			return ExecutionExecutorScope.EXECUTION_STRATEGY;
		}
		if (missingText(executionBoundary)) {
			return ExecutionExecutorScope.EXECUTION_BOUNDARY;
		}
		if (missingText(executorPolicy)) {
			return ExecutionExecutorScope.EXECUTOR_POLICY;
		}

		return ExecutionExecutorScope.EXECUTION_EXECUTOR;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
