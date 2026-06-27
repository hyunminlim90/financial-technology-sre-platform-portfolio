package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ExecutionAdapter(
		ExecutionAdapterLevel level,
		ExecutionAdapterReason reason,
		ExecutionAdapterScope scope,
		ExecutionEngineSelectorIntegrationResult executionEngineSelectorIntegration,
		String adapterIdentifier,
		String adapterType,
		String adapterBinding,
		String adapterPolicy,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ExecutionAdapter {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				executionEngineSelectorIntegration,
				"executionEngineSelectorIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualAdapterImplementation() {
		return false;
	}

	public boolean adapterInvocation() {
		return false;
	}

	public boolean kubernetesAdapter() {
		return false;
	}

	public boolean argoCdAdapter() {
		return false;
	}

	public boolean terraformOrOpenTofuAdapter() {
		return false;
	}

	public boolean sshOrAnsibleAdapter() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}
}
