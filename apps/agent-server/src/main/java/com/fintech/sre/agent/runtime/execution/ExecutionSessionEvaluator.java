package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionSessionEvaluator {

	public ExecutionSession evaluate(
			ExecutionExecutorIntegrationResult executionExecutorIntegration,
			String sessionIdentifier,
			String executionCorrelationIdentifier,
			String executionScope,
			String sessionPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				executionExecutorIntegration,
				"executionExecutorIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionSession(
				level(
						executionExecutorIntegration,
						sessionIdentifier,
						executionCorrelationIdentifier,
						executionScope,
						sessionPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						executionExecutorIntegration,
						sessionIdentifier,
						executionCorrelationIdentifier,
						executionScope,
						sessionPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						executionExecutorIntegration,
						sessionIdentifier,
						executionCorrelationIdentifier,
						executionScope,
						sessionPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				executionExecutorIntegration,
				sessionIdentifier,
				executionCorrelationIdentifier,
				executionScope,
				sessionPolicy,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionSessionLevel level(
			ExecutionExecutorIntegrationResult executionExecutorIntegration,
			String sessionIdentifier,
			String executionCorrelationIdentifier,
			String executionScope,
			String sessionPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionSessionLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionSessionLevel.BLOCKED;
		}
		if (missingText(sessionIdentifier)) {
			return ExecutionSessionLevel.BLOCKED;
		}
		if (missingText(executionCorrelationIdentifier)) {
			return ExecutionSessionLevel.BLOCKED;
		}
		if (missingText(executionScope)) {
			return ExecutionSessionLevel.BLOCKED;
		}
		if (missingText(sessionPolicy)) {
			return ExecutionSessionLevel.BLOCKED;
		}

		return switch (executionExecutorIntegration.status()) {
			case EXECUTION_EXECUTOR_READY_VIEW ->
				ExecutionSessionLevel.EXECUTION_SESSION_READY;
			case PARTIAL_EXECUTION_EXECUTOR -> ExecutionSessionLevel.PARTIAL;
			case NOT_READY -> ExecutionSessionLevel.NOT_READY;
			case UNRELIABLE -> ExecutionSessionLevel.UNRELIABLE;
			case BLOCKED -> ExecutionSessionLevel.BLOCKED;
			case UNKNOWN -> ExecutionSessionLevel.UNKNOWN;
		};
	}

	private ExecutionSessionReason reason(
			ExecutionExecutorIntegrationResult executionExecutorIntegration,
			String sessionIdentifier,
			String executionCorrelationIdentifier,
			String executionScope,
			String sessionPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionSessionReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionSessionReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(sessionIdentifier)) {
			return ExecutionSessionReason.MISSING_SESSION_IDENTIFIER;
		}
		if (missingText(executionCorrelationIdentifier)) {
			return ExecutionSessionReason.MISSING_EXECUTION_CORRELATION_IDENTIFIER;
		}
		if (missingText(executionScope)) {
			return ExecutionSessionReason.MISSING_EXECUTION_SCOPE;
		}
		if (missingText(sessionPolicy)) {
			return ExecutionSessionReason.MISSING_SESSION_POLICY;
		}

		return switch (executionExecutorIntegration.status()) {
			case EXECUTION_EXECUTOR_READY_VIEW ->
				ExecutionSessionReason.EXECUTION_EXECUTOR_READY;
			case PARTIAL_EXECUTION_EXECUTOR ->
				ExecutionSessionReason.PARTIAL_EXECUTION_EXECUTOR;
			case NOT_READY ->
				ExecutionSessionReason.NOT_READY_EXECUTION_EXECUTOR;
			case UNRELIABLE ->
				ExecutionSessionReason.UNRELIABLE_EXECUTION_EXECUTOR;
			case BLOCKED ->
				ExecutionSessionReason.BLOCKED_EXECUTION_EXECUTOR;
			case UNKNOWN -> ExecutionSessionReason.UNKNOWN;
		};
	}

	private ExecutionSessionScope scope(
			ExecutionExecutorIntegrationResult executionExecutorIntegration,
			String sessionIdentifier,
			String executionCorrelationIdentifier,
			String executionScope,
			String sessionPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionSessionScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionSessionScope.LIFECYCLE_RISK;
		}
		if (missingText(sessionIdentifier)) {
			return ExecutionSessionScope.EXECUTION_SESSION;
		}
		if (missingText(executionCorrelationIdentifier)) {
			return ExecutionSessionScope.EXECUTION_CORRELATION;
		}
		if (missingText(executionScope)) {
			return ExecutionSessionScope.EXECUTION_SCOPE;
		}
		if (missingText(sessionPolicy)) {
			return ExecutionSessionScope.SESSION_POLICY;
		}

		return ExecutionSessionScope.EXECUTION_SESSION;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
