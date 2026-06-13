package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ApprovalReliabilityEvaluator {

	public ApprovalReliability evaluate(
			RecommendationReliability recommendationReliability,
			String operatorContext
	) {
		Objects.requireNonNull(
				recommendationReliability,
				"recommendationReliability must not be null"
		);

		return new ApprovalReliability(
				level(recommendationReliability, operatorContext),
				reason(recommendationReliability, operatorContext),
				scope(recommendationReliability, operatorContext),
				recommendationReliability,
				operatorContext
		);
	}

	private ApprovalReliabilityLevel level(
			RecommendationReliability recommendationReliability,
			String operatorContext
	) {
		if (missingHumanApprovalRequirement(recommendationReliability)) {
			return ApprovalReliabilityLevel.BLOCKED;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalReliabilityLevel.BLOCKED;
		}
		if (missingRollbackBinding(recommendationReliability)) {
			return ApprovalReliabilityLevel.BLOCKED;
		}
		if (missingVerificationBinding(recommendationReliability)) {
			return ApprovalReliabilityLevel.BLOCKED;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.BLOCKED) {
			return ApprovalReliabilityLevel.BLOCKED;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.UNRELIABLE) {
			return ApprovalReliabilityLevel.UNRELIABLE;
		}
		if (contradictoryRecommendation(recommendationReliability)) {
			return ApprovalReliabilityLevel.LOW;
		}
		if (paymentSafetyUncertainty(recommendationReliability)) {
			return ApprovalReliabilityLevel.LOW;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.LOW) {
			return ApprovalReliabilityLevel.LOW;
		}
		if (highApprovalReliability(recommendationReliability, operatorContext)) {
			return ApprovalReliabilityLevel.HIGH;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.MEDIUM
				|| restrictedBindings(recommendationReliability)
				|| recommendationReliability.humanApprovalDecision().scope()
				== HumanApprovalScope.CRITICAL_EXPLICIT) {
			return ApprovalReliabilityLevel.MEDIUM;
		}
		return ApprovalReliabilityLevel.UNKNOWN;
	}

	private ApprovalReliabilityReason reason(
			RecommendationReliability recommendationReliability,
			String operatorContext
	) {
		if (missingHumanApprovalRequirement(recommendationReliability)) {
			return ApprovalReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalReliabilityReason.MISSING_OPERATOR_CONTEXT;
		}
		if (missingRollbackBinding(recommendationReliability)) {
			return ApprovalReliabilityReason.MISSING_ROLLBACK_BINDING;
		}
		if (missingVerificationBinding(recommendationReliability)) {
			return ApprovalReliabilityReason.MISSING_VERIFICATION_BINDING;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.BLOCKED) {
			return ApprovalReliabilityReason.BLOCKED_RECOMMENDATION;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.UNRELIABLE) {
			return ApprovalReliabilityReason.UNRELIABLE_RECOMMENDATION;
		}
		if (contradictoryRecommendation(recommendationReliability)) {
			return ApprovalReliabilityReason.CONTRADICTORY_RECOMMENDATION;
		}
		if (paymentSafetyUncertainty(recommendationReliability)) {
			return ApprovalReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.LOW) {
			return ApprovalReliabilityReason.LOW_RECOMMENDATION_RELIABILITY;
		}
		if (highApprovalReliability(recommendationReliability, operatorContext)) {
			return ApprovalReliabilityReason.HIGH_RECOMMENDATION_RELIABILITY;
		}
		return ApprovalReliabilityReason.UNKNOWN;
	}

	private ApprovalReliabilityScope scope(
			RecommendationReliability recommendationReliability,
			String operatorContext
	) {
		if (missingHumanApprovalRequirement(recommendationReliability)) {
			return ApprovalReliabilityScope.HUMAN_APPROVAL;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalReliabilityScope.OPERATOR_CONTEXT;
		}
		if (missingRollbackBinding(recommendationReliability)) {
			return ApprovalReliabilityScope.ROLLBACK_BOUNDARY;
		}
		if (missingVerificationBinding(recommendationReliability)) {
			return ApprovalReliabilityScope.VERIFICATION_BOUNDARY;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.BLOCKED
				|| recommendationReliability.level()
				== RecommendationReliabilityLevel.UNRELIABLE) {
			return ApprovalReliabilityScope.RECOMMENDATION;
		}
		if (paymentSafetyUncertainty(recommendationReliability)) {
			return ApprovalReliabilityScope.PAYMENT_SAFETY;
		}
		if (contradictoryRecommendation(recommendationReliability)) {
			return ApprovalReliabilityScope.LIFECYCLE;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.LOW
				|| recommendationReliability.level()
				== RecommendationReliabilityLevel.MEDIUM) {
			return ApprovalReliabilityScope.OPERATOR_VIEW;
		}
		return ApprovalReliabilityScope.RECOMMENDATION;
	}

	private boolean highApprovalReliability(
			RecommendationReliability recommendationReliability,
			String operatorContext
	) {
		return recommendationReliability.level() == RecommendationReliabilityLevel.HIGH
				&& !missingHumanApprovalRequirement(recommendationReliability)
				&& !missingOperatorContext(operatorContext)
				&& !missingRollbackBinding(recommendationReliability)
				&& !missingVerificationBinding(recommendationReliability)
				&& !paymentSafetyUncertainty(recommendationReliability)
				&& !contradictoryRecommendation(recommendationReliability);
	}

	private boolean missingHumanApprovalRequirement(
			RecommendationReliability recommendationReliability
	) {
		HumanApprovalDecision humanApprovalDecision =
				recommendationReliability.humanApprovalDecision();
		return humanApprovalDecision == null
				|| !humanApprovalDecision.approvalRequired()
				|| humanApprovalDecision.requirement() == null;
	}

	private boolean missingOperatorContext(String operatorContext) {
		return operatorContext == null || operatorContext.isBlank();
	}

	private boolean missingRollbackBinding(
			RecommendationReliability recommendationReliability
	) {
		RollbackVerificationBindingDecision decision = recommendationReliability
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE;
	}

	private boolean missingVerificationBinding(
			RecommendationReliability recommendationReliability
	) {
		RollbackVerificationBindingDecision decision = recommendationReliability
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE;
	}

	private boolean restrictedBindings(
			RecommendationReliability recommendationReliability
	) {
		RollbackVerificationBindingDecision decision = recommendationReliability
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision != null
				&& decision.status() == RollbackVerificationBindingStatus.RESTRICTED;
	}

	private boolean paymentSafetyUncertainty(
			RecommendationReliability recommendationReliability
	) {
		return recommendationReliability.reason()
				== RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
	}

	private boolean contradictoryRecommendation(
			RecommendationReliability recommendationReliability
	) {
		return recommendationReliability.reason()
				== RecommendationReliabilityReason.CONTRADICTORY_DECISION;
	}
}
