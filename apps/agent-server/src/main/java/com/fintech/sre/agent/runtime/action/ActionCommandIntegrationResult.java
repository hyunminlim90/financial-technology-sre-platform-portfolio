package com.fintech.sre.agent.runtime.action;

import java.util.Objects;

public record ActionCommandIntegrationResult(
		ActionCommand actionCommand,
		ActionCommandIntegrationStatus status,
		ActionCommandIntegrationReason reason,
		ActionCommandIntegrationScope scope,
		boolean operatorFacingActionCommandCandidateVisible,
		boolean actionCommandCandidateCertaintyAllowed
) {
	public ActionCommandIntegrationResult {
		Objects.requireNonNull(actionCommand, "actionCommand must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
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

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
