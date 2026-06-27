package com.fintech.sre.agent.runtime.action;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationResult;

public record ActionCommand(
		ActionCommandLevel level,
		ActionCommandReason reason,
		ActionCommandScope scope,
		VerificationRequestIntegrationResult verificationRequestIntegration,
		String actionCommandIdentifier,
		String actionType,
		String targetLayer,
		String blastRadiusBoundary,
		boolean rollbackBindingPresent,
		boolean verificationBindingPresent,
		OperationalUncertainty lifecycleRisk,
		boolean paymentSafetyUncertainty
) {
	public ActionCommand {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				verificationRequestIntegration,
				"verificationRequestIntegration must not be null"
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

	public boolean argoCdSync() {
		return false;
	}

	public boolean terraformApply() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}
}
