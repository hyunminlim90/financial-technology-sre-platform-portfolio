package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ActionAdmissionGate {

	public ActionAdmissionDecision evaluate(
			ActionAdmissionRequirement requirement
	) {
		Objects.requireNonNull(requirement, "requirement must not be null");

		if (!requirement.safetyPolicyDecision().admitted()) {
			return rejected(
					requirement,
					ActionAdmissionRejectionReason.SAFETY_POLICY_REJECTED
			);
		}
		if (!requirement.actionCommandEligibility().eligible()) {
			return rejected(
					requirement,
					ActionAdmissionRejectionReason.ACTION_COMMAND_BOUNDARY_REJECTED
			);
		}
		if (!requirement.recommendationEligibility().eligible()) {
			return rejected(
					requirement,
					ActionAdmissionRejectionReason.RECOMMENDATION_BOUNDARY_REJECTED
			);
		}
		if (!requirement.scenarioBindingDecision().actionCommandScenarioAvailable()) {
			return rejected(
					requirement,
					ActionAdmissionRejectionReason.SCENARIO_BINDING_REJECTED
			);
		}
		if (!requirement.rollbackVerificationBindingDecision().actionCommandAvailable()) {
			return rejected(
					requirement,
					ActionAdmissionRejectionReason
							.ROLLBACK_VERIFICATION_BINDING_REJECTED
			);
		}

		boolean restricted = requirement.safetyPolicyDecision().scope()
				== SafetyPolicyScope.RESTRICTED
				|| requirement.scenarioBindingDecision().highRiskRestricted()
				|| requirement.rollbackVerificationBindingDecision().highRiskRestricted();

		if (restricted && requirement.unrestrictedRequested()) {
			return rejected(
					requirement,
					ActionAdmissionRejectionReason
							.RESTRICTED_STATE_DISALLOWS_UNRESTRICTED_ADMISSION
			);
		}

		return new ActionAdmissionDecision(
				true,
				restricted
						? ActionAdmissionScope.RESTRICTED_CANDIDATE
						: ActionAdmissionScope.CANDIDATE,
				requirement,
				null
		);
	}

	private ActionAdmissionDecision rejected(
			ActionAdmissionRequirement requirement,
			ActionAdmissionRejectionReason rejectionReason
	) {
		return new ActionAdmissionDecision(
				false,
				ActionAdmissionScope.NONE,
				requirement,
				rejectionReason
		);
	}
}
