package com.fintech.sre.agent.runtime.readiness;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ApprovalReadinessEvaluator {

	public ApprovalReadiness evaluate(
			RecommendationReadiness recommendationReadiness,
			String operatorContext
	) {
		Objects.requireNonNull(
				recommendationReadiness,
				"recommendationReadiness must not be null"
		);

		return new ApprovalReadiness(
				level(recommendationReadiness, operatorContext),
				reason(recommendationReadiness, operatorContext),
				scope(recommendationReadiness, operatorContext),
				recommendationReadiness,
				operatorContext
		);
	}

	private ApprovalReadinessLevel level(
			RecommendationReadiness recommendationReadiness,
			String operatorContext
	) {
		if (paymentSafetyUncertainty(recommendationReadiness)) {
			return ApprovalReadinessLevel.BLOCKED;
		}
		if (recommendationReadiness.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return ApprovalReadinessLevel.BLOCKED;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalReadinessLevel.BLOCKED;
		}
		if (missingHumanApprovalRequirement(recommendationReadiness)) {
			return ApprovalReadinessLevel.BLOCKED;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.BLOCKED) {
			return ApprovalReadinessLevel.BLOCKED;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.UNRELIABLE) {
			return ApprovalReadinessLevel.UNRELIABLE;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.NOT_READY) {
			return ApprovalReadinessLevel.NOT_READY;
		}
		if (lifecycleUncertainty(recommendationReadiness)) {
			return ApprovalReadinessLevel.PARTIAL;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.PARTIAL) {
			return ApprovalReadinessLevel.PARTIAL;
		}
		if (ready(recommendationReadiness, operatorContext)) {
			return ApprovalReadinessLevel.READY;
		}
		return ApprovalReadinessLevel.UNKNOWN;
	}

	private ApprovalReadinessReason reason(
			RecommendationReadiness recommendationReadiness,
			String operatorContext
	) {
		if (paymentSafetyUncertainty(recommendationReadiness)) {
			return ApprovalReadinessReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (recommendationReadiness.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return ApprovalReadinessReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalReadinessReason.MISSING_OPERATOR_CONTEXT;
		}
		if (missingHumanApprovalRequirement(recommendationReadiness)) {
			return ApprovalReadinessReason.MISSING_HUMAN_APPROVAL_REQUIREMENT;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.BLOCKED) {
			return ApprovalReadinessReason.BLOCKED_RECOMMENDATION;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.UNRELIABLE) {
			return ApprovalReadinessReason.UNRELIABLE_RECOMMENDATION;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.NOT_READY) {
			return ApprovalReadinessReason.NOT_READY_RECOMMENDATION;
		}
		if (lifecycleUncertainty(recommendationReadiness)) {
			return ApprovalReadinessReason.LIFECYCLE_UNCERTAINTY;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.PARTIAL) {
			return ApprovalReadinessReason.PARTIAL_RECOMMENDATION;
		}
		if (ready(recommendationReadiness, operatorContext)) {
			return ApprovalReadinessReason.READY_RECOMMENDATION;
		}
		return ApprovalReadinessReason.UNKNOWN;
	}

	private ApprovalReadinessScope scope(
			RecommendationReadiness recommendationReadiness,
			String operatorContext
	) {
		if (paymentSafetyUncertainty(recommendationReadiness)) {
			return ApprovalReadinessScope.PAYMENT_SAFETY;
		}
		if (recommendationReadiness.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return ApprovalReadinessScope.LIFECYCLE_RISK;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalReadinessScope.OPERATOR_CONTEXT;
		}
		if (missingHumanApprovalRequirement(recommendationReadiness)) {
			return ApprovalReadinessScope.HUMAN_APPROVAL;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.BLOCKED
				|| recommendationReadiness.level()
				== RecommendationReadinessLevel.UNRELIABLE) {
			return ApprovalReadinessScope.RECOMMENDATION_READINESS;
		}
		if (lifecycleUncertainty(recommendationReadiness)) {
			return ApprovalReadinessScope.LIFECYCLE_UNCERTAINTY;
		}
		if (recommendationReadiness.level() == RecommendationReadinessLevel.NOT_READY
				|| recommendationReadiness.level()
				== RecommendationReadinessLevel.PARTIAL) {
			return ApprovalReadinessScope.OPERATOR_VIEW;
		}
		return ApprovalReadinessScope.RUNTIME_READINESS;
	}

	private boolean ready(
			RecommendationReadiness recommendationReadiness,
			String operatorContext
	) {
		return recommendationReadiness.level() == RecommendationReadinessLevel.READY
				&& !missingOperatorContext(operatorContext)
				&& !missingHumanApprovalRequirement(recommendationReadiness)
				&& recommendationReadiness.lifecycleRisk() != OperationalUncertainty.CRITICAL
				&& !paymentSafetyUncertainty(recommendationReadiness)
				&& !lifecycleUncertainty(recommendationReadiness);
	}

	private boolean missingOperatorContext(String operatorContext) {
		return operatorContext == null || operatorContext.isBlank();
	}

	private boolean missingHumanApprovalRequirement(
			RecommendationReadiness recommendationReadiness
	) {
		return recommendationReadiness
				.recommendationReliability()
				.humanApprovalDecision() == null
				|| !recommendationReadiness
				.recommendationReliability()
				.humanApprovalDecision()
				.approvalRequired();
	}

	private boolean paymentSafetyUncertainty(
			RecommendationReadiness recommendationReadiness
	) {
		return recommendationReadiness.reason()
				== RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY;
	}

	private boolean lifecycleUncertainty(
			RecommendationReadiness recommendationReadiness
	) {
		return recommendationReadiness.lifecycleUncertaintyDetected()
				|| recommendationReadiness.reason()
				== RecommendationReadinessReason.LIFECYCLE_UNCERTAINTY;
	}
}
