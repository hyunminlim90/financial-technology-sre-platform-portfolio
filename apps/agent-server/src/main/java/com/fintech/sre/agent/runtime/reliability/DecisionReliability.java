package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record DecisionReliability(
		DecisionReliabilityLevel level,
		DecisionReliabilityReason reason,
		DecisionReliabilityScope scope,
		AssessmentReliability assessmentReliability,
		ScenarioBindingDecision scenarioBindingDecision,
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision
) {
	public DecisionReliability {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				assessmentReliability,
				"assessmentReliability must not be null"
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmission() {
		return false;
	}

	public boolean actionDecision() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
