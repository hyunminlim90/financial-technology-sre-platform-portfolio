package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ActionAdmissionReliabilityIntegrationResult(
		ActionAdmissionReliability actionAdmissionReliability,
		EvidenceRuntimeApiResponse apiResponse,
		ActionAdmissionReliabilityIntegrationStatus status,
		ActionAdmissionReliabilityIntegrationReason reason,
		ActionAdmissionReliabilityIntegrationScope scope,
		boolean actionCommandCandidateVisible,
		boolean actionAdmissionCertaintyAllowed
) {
	public ActionAdmissionReliabilityIntegrationResult {
		Objects.requireNonNull(
				actionAdmissionReliability,
				"actionAdmissionReliability must not be null"
		);
		Objects.requireNonNull(apiResponse, "apiResponse must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesActionAdmission() {
		return false;
	}

	public boolean actualActionCommand() {
		return false;
	}

	public boolean actionAdmissionResult() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean approval() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
