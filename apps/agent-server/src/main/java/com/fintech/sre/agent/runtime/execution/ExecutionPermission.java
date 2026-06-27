package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationResult;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public record ExecutionPermission(
		ExecutionPermissionLevel level,
		ExecutionPermissionReason reason,
		ExecutionPermissionScope scope,
		ActionCommandIntegrationResult actionCommandIntegration,
		String executionPermissionIdentifier,
		String executionPolicy,
		String operatorAuthorization,
		boolean executionGuardrailPresent,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ExecutionPermission {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				actionCommandIntegration,
				"actionCommandIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean actionDispatch() {
		return false;
	}

	public boolean kubernetesApiCall() {
		return false;
	}

	public boolean kubectlExecution() {
		return false;
	}

	public boolean argoCdSync() {
		return false;
	}

	public boolean terraformApply() {
		return false;
	}

	public boolean sshOrAnsibleExecution() {
		return false;
	}
}
