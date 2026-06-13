package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class RecommendationReliabilityEvaluator {

	public RecommendationReliability evaluate(
			DecisionReliability decisionReliability,
			HumanApprovalDecision humanApprovalDecision
	) {
		Objects.requireNonNull(
				decisionReliability,
				"decisionReliability must not be null"
		);

		return new RecommendationReliability(
				level(decisionReliability, humanApprovalDecision),
				reason(decisionReliability, humanApprovalDecision),
				scope(decisionReliability, humanApprovalDecision),
				decisionReliability,
				humanApprovalDecision
		);
	}

	private RecommendationReliabilityLevel level(
			DecisionReliability decisionReliability,
			HumanApprovalDecision humanApprovalDecision
	) {
		if (missingHumanApprovalRequirement(humanApprovalDecision)) {
			return RecommendationReliabilityLevel.BLOCKED;
		}
		if (missingRollbackBinding(decisionReliability)) {
			return RecommendationReliabilityLevel.BLOCKED;
		}
		if (missingVerificationBinding(decisionReliability)) {
			return RecommendationReliabilityLevel.BLOCKED;
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.BLOCKED) {
			return RecommendationReliabilityLevel.BLOCKED;
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.UNRELIABLE) {
			return RecommendationReliabilityLevel.UNRELIABLE;
		}
		if (contradictoryDecision(decisionReliability)) {
			return RecommendationReliabilityLevel.LOW;
		}
		if (paymentSafetyUncertainty(decisionReliability)) {
			return RecommendationReliabilityLevel.LOW;
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.LOW) {
			return RecommendationReliabilityLevel.LOW;
		}
		if (highRecommendationReliability(decisionReliability, humanApprovalDecision)) {
			return RecommendationReliabilityLevel.HIGH;
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.MEDIUM
				|| restrictedBindings(decisionReliability)
				|| humanApprovalDecision.scope() == HumanApprovalScope.CRITICAL_EXPLICIT) {
			return RecommendationReliabilityLevel.MEDIUM;
		}
		return RecommendationReliabilityLevel.UNKNOWN;
	}

	private RecommendationReliabilityReason reason(
			DecisionReliability decisionReliability,
			HumanApprovalDecision humanApprovalDecision
	) {
		if (missingHumanApprovalRequirement(humanApprovalDecision)) {
			return RecommendationReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT;
		}
		if (missingRollbackBinding(decisionReliability)) {
			return RecommendationReliabilityReason.MISSING_ROLLBACK_BINDING;
		}
		if (missingVerificationBinding(decisionReliability)) {
			return RecommendationReliabilityReason.MISSING_VERIFICATION_BINDING;
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.BLOCKED) {
			return RecommendationReliabilityReason.BLOCKED_DECISION;
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.UNRELIABLE) {
			return RecommendationReliabilityReason.UNRELIABLE_DECISION;
		}
		if (contradictoryDecision(decisionReliability)) {
			return RecommendationReliabilityReason.CONTRADICTORY_DECISION;
		}
		if (paymentSafetyUncertainty(decisionReliability)) {
			return RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.LOW) {
			return RecommendationReliabilityReason.LOW_DECISION_RELIABILITY;
		}
		if (highRecommendationReliability(decisionReliability, humanApprovalDecision)) {
			return RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY;
		}
		return RecommendationReliabilityReason.UNKNOWN;
	}

	private RecommendationReliabilityScope scope(
			DecisionReliability decisionReliability,
			HumanApprovalDecision humanApprovalDecision
	) {
		if (missingHumanApprovalRequirement(humanApprovalDecision)) {
			return RecommendationReliabilityScope.HUMAN_APPROVAL;
		}
		if (missingRollbackBinding(decisionReliability)) {
			return RecommendationReliabilityScope.ROLLBACK_BOUNDARY;
		}
		if (missingVerificationBinding(decisionReliability)) {
			return RecommendationReliabilityScope.VERIFICATION_BOUNDARY;
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.BLOCKED
				|| decisionReliability.level() == DecisionReliabilityLevel.UNRELIABLE) {
			return RecommendationReliabilityScope.DECISION;
		}
		if (paymentSafetyUncertainty(decisionReliability)) {
			return RecommendationReliabilityScope.PAYMENT_SAFETY;
		}
		if (contradictoryDecision(decisionReliability)) {
			return RecommendationReliabilityScope.LIFECYCLE;
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.LOW
				|| decisionReliability.level() == DecisionReliabilityLevel.MEDIUM) {
			return RecommendationReliabilityScope.OPERATOR_VIEW;
		}
		return RecommendationReliabilityScope.DECISION;
	}

	private boolean highRecommendationReliability(
			DecisionReliability decisionReliability,
			HumanApprovalDecision humanApprovalDecision
	) {
		return decisionReliability.level() == DecisionReliabilityLevel.HIGH
				&& humanApprovalDecision != null
				&& humanApprovalDecision.approvalRequired()
				&& !missingRollbackBinding(decisionReliability)
				&& !missingVerificationBinding(decisionReliability)
				&& !paymentSafetyUncertainty(decisionReliability)
				&& !contradictoryDecision(decisionReliability);
	}

	private boolean missingHumanApprovalRequirement(
			HumanApprovalDecision humanApprovalDecision
	) {
		return humanApprovalDecision == null
				|| !humanApprovalDecision.approvalRequired()
				|| humanApprovalDecision.requirement() == null;
	}

	private boolean missingRollbackBinding(
			DecisionReliability decisionReliability
	) {
		RollbackVerificationBindingDecision decision =
				decisionReliability.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE;
	}

	private boolean missingVerificationBinding(
			DecisionReliability decisionReliability
	) {
		RollbackVerificationBindingDecision decision =
				decisionReliability.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE;
	}

	private boolean restrictedBindings(
			DecisionReliability decisionReliability
	) {
		RollbackVerificationBindingDecision decision =
				decisionReliability.rollbackVerificationBindingDecision();
		return decision != null
				&& decision.status() == RollbackVerificationBindingStatus.RESTRICTED;
	}

	private boolean paymentSafetyUncertainty(
			DecisionReliability decisionReliability
	) {
		return decisionReliability.reason()
				== DecisionReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
	}

	private boolean contradictoryDecision(
			DecisionReliability decisionReliability
	) {
		return decisionReliability.reason()
				== DecisionReliabilityReason.CONTRADICTORY_ASSESSMENT;
	}
}
