package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ScenarioBindingDecision(
		ScenarioBindingStatus status,
		ScenarioReference scenarioReference,
		ScenarioBindingRejectionReason rejectionReason
) {
	public ScenarioBindingDecision {
		Objects.requireNonNull(status, "status must not be null");
		if (status == ScenarioBindingStatus.REJECTED && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected scenario binding requires rejection reason"
			);
		}
		if (status == ScenarioBindingStatus.BOUND && rejectionReason != null) {
			throw new IllegalArgumentException(
					"bound scenario binding must not contain rejection reason"
			);
		}
	}

	public boolean recommendationScenarioAvailable() {
		return status == ScenarioBindingStatus.BOUND
				|| status == ScenarioBindingStatus.RESTRICTED;
	}

	public boolean actionCommandScenarioAvailable() {
		return status == ScenarioBindingStatus.BOUND
				|| status == ScenarioBindingStatus.RESTRICTED;
	}

	public boolean highRiskRestricted() {
		return status == ScenarioBindingStatus.RESTRICTED;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean semanticPrerequisiteOnly() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
