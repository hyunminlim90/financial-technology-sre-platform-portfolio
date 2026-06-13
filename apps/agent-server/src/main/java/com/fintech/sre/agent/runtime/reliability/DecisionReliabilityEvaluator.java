package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class DecisionReliabilityEvaluator {

	public DecisionReliability evaluate(
			AssessmentReliability assessmentReliability,
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision
	) {
		Objects.requireNonNull(
				assessmentReliability,
				"assessmentReliability must not be null"
		);

		return new DecisionReliability(
				level(
						assessmentReliability,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision
				),
				reason(
						assessmentReliability,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision
				),
				scope(
						assessmentReliability,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision
				),
				assessmentReliability,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision
		);
	}

	private DecisionReliabilityLevel level(
			AssessmentReliability assessmentReliability,
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision
	) {
		if (assessmentReliability.level() == AssessmentReliabilityLevel.BLOCKED) {
			return DecisionReliabilityLevel.BLOCKED;
		}
		if (missingScenarioBinding(scenarioBindingDecision)) {
			return DecisionReliabilityLevel.BLOCKED;
		}
		if (missingRollbackBinding(rollbackVerificationBindingDecision)) {
			return DecisionReliabilityLevel.BLOCKED;
		}
		if (missingVerificationBinding(rollbackVerificationBindingDecision)) {
			return DecisionReliabilityLevel.BLOCKED;
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.UNRELIABLE) {
			return DecisionReliabilityLevel.UNRELIABLE;
		}
		if (contradictoryAssessment(assessmentReliability)) {
			return DecisionReliabilityLevel.LOW;
		}
		if (paymentSafetyUncertainty(assessmentReliability)) {
			return DecisionReliabilityLevel.LOW;
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.LOW) {
			return DecisionReliabilityLevel.LOW;
		}
		if (highDecisionReliability(
				assessmentReliability,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision
		)) {
			return DecisionReliabilityLevel.HIGH;
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.MEDIUM
				|| restrictedScenario(scenarioBindingDecision)
				|| restrictedBindings(rollbackVerificationBindingDecision)) {
			return DecisionReliabilityLevel.MEDIUM;
		}
		return DecisionReliabilityLevel.UNKNOWN;
	}

	private DecisionReliabilityReason reason(
			AssessmentReliability assessmentReliability,
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision
	) {
		if (assessmentReliability.level() == AssessmentReliabilityLevel.BLOCKED) {
			return DecisionReliabilityReason.BLOCKED_ASSESSMENT;
		}
		if (missingScenarioBinding(scenarioBindingDecision)) {
			return DecisionReliabilityReason.MISSING_SCENARIO_BINDING;
		}
		if (missingRollbackBinding(rollbackVerificationBindingDecision)) {
			return DecisionReliabilityReason.MISSING_ROLLBACK_BINDING;
		}
		if (missingVerificationBinding(rollbackVerificationBindingDecision)) {
			return DecisionReliabilityReason.MISSING_VERIFICATION_BINDING;
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.UNRELIABLE) {
			return DecisionReliabilityReason.UNRELIABLE_ASSESSMENT;
		}
		if (contradictoryAssessment(assessmentReliability)) {
			return DecisionReliabilityReason.CONTRADICTORY_ASSESSMENT;
		}
		if (paymentSafetyUncertainty(assessmentReliability)) {
			return DecisionReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.LOW) {
			return DecisionReliabilityReason.LOW_ASSESSMENT_RELIABILITY;
		}
		if (highDecisionReliability(
				assessmentReliability,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision
		)) {
			return DecisionReliabilityReason.HIGH_ASSESSMENT_RELIABILITY;
		}
		return DecisionReliabilityReason.UNKNOWN;
	}

	private DecisionReliabilityScope scope(
			AssessmentReliability assessmentReliability,
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision
	) {
		if (assessmentReliability.level() == AssessmentReliabilityLevel.BLOCKED
				|| assessmentReliability.level() == AssessmentReliabilityLevel.UNRELIABLE) {
			return DecisionReliabilityScope.ASSESSMENT;
		}
		if (missingScenarioBinding(scenarioBindingDecision)) {
			return DecisionReliabilityScope.SCENARIO_BOUNDARY;
		}
		if (missingRollbackBinding(rollbackVerificationBindingDecision)) {
			return DecisionReliabilityScope.ROLLBACK_BOUNDARY;
		}
		if (missingVerificationBinding(rollbackVerificationBindingDecision)) {
			return DecisionReliabilityScope.VERIFICATION_BOUNDARY;
		}
		if (paymentSafetyUncertainty(assessmentReliability)) {
			return DecisionReliabilityScope.PAYMENT_SAFETY;
		}
		if (contradictoryAssessment(assessmentReliability)) {
			return DecisionReliabilityScope.LIFECYCLE;
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.LOW
				|| assessmentReliability.level() == AssessmentReliabilityLevel.MEDIUM) {
			return DecisionReliabilityScope.OPERATOR_VIEW;
		}
		return DecisionReliabilityScope.ASSESSMENT;
	}

	private boolean highDecisionReliability(
			AssessmentReliability assessmentReliability,
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision
	) {
		return assessmentReliability.level() == AssessmentReliabilityLevel.HIGH
				&& scenarioBindingDecision != null
				&& scenarioBindingDecision.status() == ScenarioBindingStatus.BOUND
				&& rollbackVerificationBindingDecision != null
				&& rollbackVerificationBindingDecision.status()
				== RollbackVerificationBindingStatus.BOUND
				&& !paymentSafetyUncertainty(assessmentReliability)
				&& !contradictoryAssessment(assessmentReliability);
	}

	private boolean paymentSafetyUncertainty(
			AssessmentReliability assessmentReliability
	) {
		return assessmentReliability.reason()
				== AssessmentReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
	}

	private boolean contradictoryAssessment(
			AssessmentReliability assessmentReliability
	) {
		return assessmentReliability.reason()
				== AssessmentReliabilityReason.CONTRADICTORY_EVIDENCE;
	}

	private boolean missingScenarioBinding(
			ScenarioBindingDecision scenarioBindingDecision
	) {
		return scenarioBindingDecision == null
				|| scenarioBindingDecision.status() == ScenarioBindingStatus.REJECTED;
	}

	private boolean restrictedScenario(
			ScenarioBindingDecision scenarioBindingDecision
	) {
		return scenarioBindingDecision != null
				&& scenarioBindingDecision.status() == ScenarioBindingStatus.RESTRICTED;
	}

	private boolean missingRollbackBinding(
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision
	) {
		return rollbackVerificationBindingDecision == null
				|| rollbackVerificationBindingDecision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE;
	}

	private boolean missingVerificationBinding(
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision
	) {
		return rollbackVerificationBindingDecision == null
				|| rollbackVerificationBindingDecision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE;
	}

	private boolean restrictedBindings(
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision
	) {
		return rollbackVerificationBindingDecision != null
				&& rollbackVerificationBindingDecision.status()
				== RollbackVerificationBindingStatus.RESTRICTED;
	}
}
