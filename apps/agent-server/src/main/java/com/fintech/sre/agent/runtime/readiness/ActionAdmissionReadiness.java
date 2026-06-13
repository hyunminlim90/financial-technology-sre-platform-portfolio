package com.fintech.sre.agent.runtime.readiness;

import java.util.Objects;

public record ActionAdmissionReadiness(
		ActionAdmissionReadinessLevel level,
		ActionAdmissionReadinessReason reason,
		ActionAdmissionReadinessScope scope,
		VerificationReadiness verificationReadiness,
		String actionType,
		String blastRadiusBoundary
) {
	public ActionAdmissionReadiness {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				verificationReadiness,
				"verificationReadiness must not be null"
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualActionCommandGeneration() {
		return false;
	}

	public boolean actualActionAdmissionResult() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean approvalGeneration() {
		return false;
	}
}
