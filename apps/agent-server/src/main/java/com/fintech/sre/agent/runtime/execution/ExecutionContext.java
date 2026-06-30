package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ExecutionContext(
		ExecutionContextLevel level,
		ExecutionContextReason reason,
		ExecutionContextScope scope,
		ExecutionSessionIntegrationResult executionSessionIntegration,
		String contextIdentifier,
		String executionContextScope,
		String executionMetadata,
		String contextPolicy,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ExecutionContext {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				executionSessionIntegration,
				"executionSessionIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualContextCreation() {
		return false;
	}

	public boolean threadLocalCreation() {
		return false;
	}

	public boolean securityContextCreation() {
		return false;
	}

	public boolean transactionContextCreation() {
		return false;
	}

	public boolean kubernetesContextCreation() {
		return false;
	}

	public boolean runtimeExecution() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}
}
