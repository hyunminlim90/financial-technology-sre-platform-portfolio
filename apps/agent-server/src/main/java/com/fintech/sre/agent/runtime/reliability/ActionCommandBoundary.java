package com.fintech.sre.agent.runtime.reliability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ActionCommandBoundary {

	public ActionCommandEligibility evaluate(
			ReliabilityAssessmentResult assessmentResult,
			ReliabilityRiskClassification riskClassification,
			HumanApprovalDecision humanApprovalDecision,
			RecommendationEligibility recommendationEligibility
	) {
		Objects.requireNonNull(
				assessmentResult,
				"assessmentResult must not be null"
		);
		Objects.requireNonNull(
				riskClassification,
				"riskClassification must not be null"
		);
		Objects.requireNonNull(
				humanApprovalDecision,
				"humanApprovalDecision must not be null"
		);
		Objects.requireNonNull(
				recommendationEligibility,
				"recommendationEligibility must not be null"
		);

		List<ActionCommandRestriction> restrictions = new ArrayList<>();
		List<ActionCommandBoundaryReason> reasons = new ArrayList<>();
		restrictions.add(ActionCommandRestriction.RECOMMENDATION_IS_NOT_ACTION_COMMAND);
		restrictions.add(
				ActionCommandRestriction.RECOMMENDATION_ELIGIBILITY_IS_NOT_ACTION_ELIGIBILITY
		);
		reasons.add(ActionCommandBoundaryReason.RECOMMENDATION_IS_NOT_ACTION_COMMAND);
		reasons.add(
				ActionCommandBoundaryReason
						.RECOMMENDATION_ELIGIBILITY_DOES_NOT_GRANT_ACTION_ELIGIBILITY
		);

		boolean rollbackRequirement = recommendationEligibility.rollbackRequirement();
		boolean verificationRequirement =
				recommendationEligibility.verificationRequirement();
		boolean humanApprovalSatisfied = !humanApprovalDecision.approvalRequired();

		if (noActionCommandScenario(assessmentResult, recommendationEligibility)) {
			restrictions.add(ActionCommandRestriction.NO_ACTION_COMMAND_AVAILABLE);
			reasons.add(ActionCommandBoundaryReason.NO_ACTION_COMMAND_SCENARIO);
			return rejected(
					rollbackRequirement,
					verificationRequirement,
					humanApprovalSatisfied,
					restrictions,
					reasons
			);
		}

		if (recommendationEligibility.advisoryOnly()) {
			restrictions.add(
					ActionCommandRestriction.ADVISORY_ONLY_RECOMMENDATION_NOT_EXECUTABLE
			);
			reasons.add(
					ActionCommandBoundaryReason.ADVISORY_RECOMMENDATION_CANNOT_EXECUTE
			);
		}

		if (humanApprovalDecision.approvalRequired()) {
			restrictions.add(
					ActionCommandRestriction
							.HUMAN_APPROVAL_REQUIRED_BLOCKS_AUTOMATIC_ACTION_COMMAND
			);
			reasons.add(
					ActionCommandBoundaryReason
							.HUMAN_APPROVAL_REQUIRED_PREVENTS_AUTOMATIC_ACTION_COMMAND
			);
		}

		if (!rollbackRequirement) {
			restrictions.add(ActionCommandRestriction.MISSING_ROLLBACK_REQUIREMENT);
			reasons.add(ActionCommandBoundaryReason.ROLLBACK_REQUIREMENT_MISSING);
		}

		if (!verificationRequirement) {
			restrictions.add(ActionCommandRestriction.MISSING_VERIFICATION_REQUIREMENT);
			reasons.add(ActionCommandBoundaryReason.VERIFICATION_REQUIREMENT_MISSING);
		}

		if (assessmentResult.evidenceCorrelation().paymentSafetyUncertain()) {
			restrictions.add(
					ActionCommandRestriction.PAYMENT_SAFETY_UNCERTAINTY_BLOCKS_ACTION_COMMAND
			);
			reasons.add(
					ActionCommandBoundaryReason
							.PAYMENT_SAFETY_UNCERTAINTY_PREVENTS_ACTION_COMMAND
			);
		}

		if (assessmentResult.evidenceCorrelation().contradictoryEvidence()) {
			restrictions.add(
					ActionCommandRestriction.CONTRADICTORY_EVIDENCE_BLOCKS_ACTION_COMMAND
			);
			reasons.add(
					ActionCommandBoundaryReason
							.CONTRADICTORY_EVIDENCE_PREVENTS_ACTION_COMMAND
			);
		}

		if (assessmentResult.runtimeState() == RuntimeState.FAILED
				|| riskClassification.level() == ReliabilityRiskLevel.CRITICAL) {
			restrictions.add(
					ActionCommandRestriction
							.FAILED_OR_CRITICAL_UNRESTRICTED_ACTION_COMMAND_PROHIBITED
			);
			reasons.add(
					ActionCommandBoundaryReason
							.FAILED_OR_CRITICAL_STATE_PREVENTS_UNRESTRICTED_ACTION_COMMAND
			);
		}

		return rejected(
				rollbackRequirement,
				verificationRequirement,
				humanApprovalSatisfied,
				restrictions,
				reasons
		);
	}

	private boolean noActionCommandScenario(
			ReliabilityAssessmentResult assessmentResult,
			RecommendationEligibility recommendationEligibility
	) {
		return !recommendationEligibility.eligible()
				|| assessmentResult.runtimeState() == RuntimeState.UNKNOWN
				|| assessmentResult.evidenceCorrelation().completeness()
						== EvidenceCompleteness.ABSENT;
	}

	private ActionCommandEligibility rejected(
			boolean rollbackRequirement,
			boolean verificationRequirement,
			boolean humanApprovalSatisfied,
			List<ActionCommandRestriction> restrictions,
			List<ActionCommandBoundaryReason> reasons
	) {
		return new ActionCommandEligibility(
				false,
				new ActionCommandRequirement(
						rollbackRequirement,
						verificationRequirement,
						humanApprovalSatisfied
				),
				restrictions,
				reasons
		);
	}
}
