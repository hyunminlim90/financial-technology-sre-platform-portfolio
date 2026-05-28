package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class SafetyPolicyGate {

	public SafetyPolicyDecision evaluate(
			SafetyPolicyRequirement requirement
	) {
		Objects.requireNonNull(requirement, "requirement must not be null");

		ScenarioBindingDecision scenarioBindingDecision =
				requirement.scenarioBindingDecision();
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision =
				requirement.rollbackVerificationBindingDecision();
		ReliabilityAssessmentResult assessmentResult =
				requirement.assessmentResult();
		ReliabilityRiskClassification riskClassification =
				requirement.riskClassification();

		if (!scenarioBindingDecision.actionCommandScenarioAvailable()) {
			return rejected(requirement, SafetyPolicyRejectionReason.NO_SCENARIO);
		}

		if (rollbackVerificationBindingDecision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE) {
			return rejected(
					requirement,
					SafetyPolicyRejectionReason.MISSING_ROLLBACK_BINDING
			);
		}
		if (rollbackVerificationBindingDecision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE) {
			return rejected(
					requirement,
					SafetyPolicyRejectionReason.MISSING_VERIFICATION_BINDING
			);
		}
		if (rollbackVerificationBindingDecision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.UNKNOWN_ROLLBACK_REFERENCE) {
			return rejected(
					requirement,
					SafetyPolicyRejectionReason.UNKNOWN_ROLLBACK_BINDING
			);
		}
		if (rollbackVerificationBindingDecision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.UNKNOWN_VERIFICATION_REFERENCE) {
			return rejected(
					requirement,
					SafetyPolicyRejectionReason.UNKNOWN_VERIFICATION_BINDING
			);
		}
		if (requirement.paymentSafetyAction()
				&& rollbackVerificationBindingDecision.rejectionReason()
						== RollbackVerificationBindingRejectionReason
								.MISSING_PAYMENT_CONSISTENCY_VERIFICATION) {
			return rejected(
					requirement,
					SafetyPolicyRejectionReason
							.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
			);
		}

		if (assessmentResult.evidenceCorrelation().paymentSafetyUncertain()) {
			return rejected(
					requirement,
					SafetyPolicyRejectionReason.PAYMENT_SAFETY_UNCERTAINTY
			);
		}
		if (assessmentResult.evidenceCorrelation().contradictoryEvidence()) {
			return rejected(
					requirement,
					SafetyPolicyRejectionReason.CONTRADICTORY_EVIDENCE
			);
		}

		if (riskClassification.level() == ReliabilityRiskLevel.CRITICAL
				&& !requirement.explicitApprovalProvided()) {
			return rejected(
					requirement,
					SafetyPolicyRejectionReason.CRITICAL_EXPLICIT_APPROVAL_MISSING
			);
		}
		if (riskClassification.level().ordinal() >= ReliabilityRiskLevel.HIGH.ordinal()
				&& !requirement.approvalProvided()) {
			return rejected(
					requirement,
					SafetyPolicyRejectionReason.HIGH_RISK_APPROVAL_MISSING
			);
		}

		if (scenarioBindingDecision.highRiskRestricted()
				|| rollbackVerificationBindingDecision.highRiskRestricted()) {
			return new SafetyPolicyDecision(
					true,
					SafetyPolicyScope.RESTRICTED,
					requirement,
					null
			);
		}

		return new SafetyPolicyDecision(
				true,
				SafetyPolicyScope.STANDARD,
				requirement,
				null
		);
	}

	private SafetyPolicyDecision rejected(
			SafetyPolicyRequirement requirement,
			SafetyPolicyRejectionReason rejectionReason
	) {
		return new SafetyPolicyDecision(
				false,
				SafetyPolicyScope.NONE,
				requirement,
				rejectionReason
		);
	}
}
