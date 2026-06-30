package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionContextEvaluator {

	public ExecutionContext evaluate(
			ExecutionSessionIntegrationResult executionSessionIntegration,
			String contextIdentifier,
			String executionContextScope,
			String executionMetadata,
			String contextPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				executionSessionIntegration,
				"executionSessionIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionContext(
				level(
						executionSessionIntegration,
						contextIdentifier,
						executionContextScope,
						executionMetadata,
						contextPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						executionSessionIntegration,
						contextIdentifier,
						executionContextScope,
						executionMetadata,
						contextPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						executionSessionIntegration,
						contextIdentifier,
						executionContextScope,
						executionMetadata,
						contextPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				executionSessionIntegration,
				contextIdentifier,
				executionContextScope,
				executionMetadata,
				contextPolicy,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionContextLevel level(
			ExecutionSessionIntegrationResult executionSessionIntegration,
			String contextIdentifier,
			String executionContextScope,
			String executionMetadata,
			String contextPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionContextLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionContextLevel.BLOCKED;
		}
		if (missingText(contextIdentifier)) {
			return ExecutionContextLevel.BLOCKED;
		}
		if (missingText(executionContextScope)) {
			return ExecutionContextLevel.BLOCKED;
		}
		if (missingText(executionMetadata)) {
			return ExecutionContextLevel.BLOCKED;
		}
		if (missingText(contextPolicy)) {
			return ExecutionContextLevel.BLOCKED;
		}

		return switch (executionSessionIntegration.status()) {
			case EXECUTION_SESSION_READY_VIEW ->
				ExecutionContextLevel.EXECUTION_CONTEXT_READY;
			case PARTIAL_EXECUTION_SESSION -> ExecutionContextLevel.PARTIAL;
			case NOT_READY -> ExecutionContextLevel.NOT_READY;
			case UNRELIABLE -> ExecutionContextLevel.UNRELIABLE;
			case BLOCKED -> ExecutionContextLevel.BLOCKED;
			case UNKNOWN -> ExecutionContextLevel.UNKNOWN;
		};
	}

	private ExecutionContextReason reason(
			ExecutionSessionIntegrationResult executionSessionIntegration,
			String contextIdentifier,
			String executionContextScope,
			String executionMetadata,
			String contextPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionContextReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionContextReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(contextIdentifier)) {
			return ExecutionContextReason.MISSING_CONTEXT_IDENTIFIER;
		}
		if (missingText(executionContextScope)) {
			return ExecutionContextReason.MISSING_EXECUTION_CONTEXT_SCOPE;
		}
		if (missingText(executionMetadata)) {
			return ExecutionContextReason.MISSING_EXECUTION_METADATA;
		}
		if (missingText(contextPolicy)) {
			return ExecutionContextReason.MISSING_CONTEXT_POLICY;
		}

		return switch (executionSessionIntegration.status()) {
			case EXECUTION_SESSION_READY_VIEW ->
				ExecutionContextReason.EXECUTION_SESSION_READY;
			case PARTIAL_EXECUTION_SESSION ->
				ExecutionContextReason.PARTIAL_EXECUTION_SESSION;
			case NOT_READY ->
				ExecutionContextReason.NOT_READY_EXECUTION_SESSION;
			case UNRELIABLE ->
				ExecutionContextReason.UNRELIABLE_EXECUTION_SESSION;
			case BLOCKED ->
				ExecutionContextReason.BLOCKED_EXECUTION_SESSION;
			case UNKNOWN -> ExecutionContextReason.UNKNOWN;
		};
	}

	private ExecutionContextScope scope(
			ExecutionSessionIntegrationResult executionSessionIntegration,
			String contextIdentifier,
			String executionContextScope,
			String executionMetadata,
			String contextPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionContextScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionContextScope.LIFECYCLE_RISK;
		}
		if (missingText(contextIdentifier)) {
			return ExecutionContextScope.EXECUTION_CONTEXT;
		}
		if (missingText(executionContextScope)) {
			return ExecutionContextScope.EXECUTION_CONTEXT_SCOPE;
		}
		if (missingText(executionMetadata)) {
			return ExecutionContextScope.EXECUTION_METADATA;
		}
		if (missingText(contextPolicy)) {
			return ExecutionContextScope.CONTEXT_POLICY;
		}

		return ExecutionContextScope.EXECUTION_CONTEXT;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
