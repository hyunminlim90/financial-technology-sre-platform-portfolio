package com.fintech.sre.agent.runtime.reliability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HumanApprovalPolicy {

	public HumanApprovalDecision evaluate(
			ReliabilityAssessmentResult assessmentResult,
			ReliabilityRiskClassification riskClassification
	) {
		Objects.requireNonNull(
				assessmentResult,
				"assessmentResult must not be null"
		);
		Objects.requireNonNull(
				riskClassification,
				"riskClassification must not be null"
		);

		List<HumanApprovalReason> reasons = new ArrayList<>();
		reasons.add(HumanApprovalReason.AI_ONLY_APPROVAL_IS_NOT_ALLOWED);

		if (riskClassification.level() == ReliabilityRiskLevel.CRITICAL) {
			reasons.add(
					HumanApprovalReason.CRITICAL_RISK_REQUIRES_EXPLICIT_APPROVAL
			);
			return new HumanApprovalDecision(
					true,
					HumanApprovalScope.CRITICAL_EXPLICIT,
					new HumanApprovalRequirement(true, true, true, false),
					reasons
			);
		}

		if (requiresPaymentSafetyApproval(assessmentResult, riskClassification)) {
			reasons.add(
					HumanApprovalReason.PAYMENT_SAFETY_UNCERTAINTY_REQUIRES_APPROVAL
			);
			reasons.add(HumanApprovalReason.HIGH_RISK_REQUIRES_HUMAN_APPROVAL);
			return new HumanApprovalDecision(
					true,
					HumanApprovalScope.REQUIRED,
					new HumanApprovalRequirement(true, true, false, false),
					reasons
			);
		}

		if (requiresContradictoryEvidenceApproval(
				assessmentResult,
				riskClassification
		)) {
			reasons.add(
					HumanApprovalReason.CONTRADICTORY_EVIDENCE_REQUIRES_APPROVAL
			);
			reasons.add(HumanApprovalReason.HIGH_RISK_REQUIRES_HUMAN_APPROVAL);
			return new HumanApprovalDecision(
					true,
					HumanApprovalScope.REQUIRED,
					new HumanApprovalRequirement(true, true, false, false),
					reasons
			);
		}

		if (riskClassification.level() == ReliabilityRiskLevel.HIGH) {
			reasons.add(HumanApprovalReason.HIGH_RISK_REQUIRES_HUMAN_APPROVAL);
			return new HumanApprovalDecision(
					true,
					HumanApprovalScope.REQUIRED,
					new HumanApprovalRequirement(true, true, false, false),
					reasons
			);
		}

		if (riskClassification.level() == ReliabilityRiskLevel.MEDIUM) {
			reasons.add(
					HumanApprovalReason
							.MEDIUM_RISK_IS_CONTEXT_DEPENDENT_REVIEW_CANDIDATE
			);
			return new HumanApprovalDecision(
					false,
					HumanApprovalScope.CONTEXT_DEPENDENT_REVIEW,
					new HumanApprovalRequirement(false, true, false, false),
					reasons
			);
		}

		reasons.add(HumanApprovalReason.LOW_RISK_APPROVAL_IS_OPTIONAL);
		return new HumanApprovalDecision(
				false,
				HumanApprovalScope.OPTIONAL,
				new HumanApprovalRequirement(false, false, false, false),
				reasons
		);
	}

	private boolean requiresPaymentSafetyApproval(
			ReliabilityAssessmentResult assessmentResult,
			ReliabilityRiskClassification riskClassification
	) {
		return assessmentResult.evidenceCorrelation().paymentSafetyUncertain()
				|| riskClassification.factors()
						.contains(
								ReliabilityRiskFactor.PAYMENT_SAFETY_UNCERTAINTY
						);
	}

	private boolean requiresContradictoryEvidenceApproval(
			ReliabilityAssessmentResult assessmentResult,
			ReliabilityRiskClassification riskClassification
	) {
		return assessmentResult.evidenceCorrelation().contradictoryEvidence()
				|| riskClassification.factors()
						.contains(ReliabilityRiskFactor.CONTRADICTORY_EVIDENCE);
	}
}
