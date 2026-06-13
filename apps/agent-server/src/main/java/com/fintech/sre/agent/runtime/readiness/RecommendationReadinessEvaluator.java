package com.fintech.sre.agent.runtime.readiness;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliability;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliabilityLevel;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliabilityReason;

public class RecommendationReadinessEvaluator {

	public RecommendationReadiness evaluate(
			RecommendationReliability recommendationReliability,
			OperationalUncertainty lifecycleRisk,
			boolean lifecycleUncertaintyDetected
	) {
		Objects.requireNonNull(
				recommendationReliability,
				"recommendationReliability must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new RecommendationReadiness(
				level(
						recommendationReliability,
						lifecycleRisk,
						lifecycleUncertaintyDetected
				),
				reason(
						recommendationReliability,
						lifecycleRisk,
						lifecycleUncertaintyDetected
				),
				scope(
						recommendationReliability,
						lifecycleRisk,
						lifecycleUncertaintyDetected
				),
				recommendationReliability,
				lifecycleRisk,
				lifecycleUncertaintyDetected
		);
	}

	private RecommendationReadinessLevel level(
			RecommendationReliability recommendationReliability,
			OperationalUncertainty lifecycleRisk,
			boolean lifecycleUncertaintyDetected
	) {
		if (paymentSafetyUncertainty(recommendationReliability)) {
			return RecommendationReadinessLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return RecommendationReadinessLevel.BLOCKED;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.BLOCKED) {
			return RecommendationReadinessLevel.BLOCKED;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.UNRELIABLE) {
			return RecommendationReadinessLevel.UNRELIABLE;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.LOW) {
			return RecommendationReadinessLevel.NOT_READY;
		}
		if (lifecycleUncertaintyDetected) {
			return RecommendationReadinessLevel.PARTIAL;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.MEDIUM) {
			return RecommendationReadinessLevel.PARTIAL;
		}
		if (ready(
				recommendationReliability,
				lifecycleRisk,
				lifecycleUncertaintyDetected
		)) {
			return RecommendationReadinessLevel.READY;
		}
		return RecommendationReadinessLevel.UNKNOWN;
	}

	private RecommendationReadinessReason reason(
			RecommendationReliability recommendationReliability,
			OperationalUncertainty lifecycleRisk,
			boolean lifecycleUncertaintyDetected
	) {
		if (paymentSafetyUncertainty(recommendationReliability)) {
			return RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return RecommendationReadinessReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.BLOCKED) {
			return RecommendationReadinessReason.BLOCKED_RECOMMENDATION_RELIABILITY;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.UNRELIABLE) {
			return RecommendationReadinessReason.UNRELIABLE_RECOMMENDATION_RELIABILITY;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.LOW) {
			return RecommendationReadinessReason.LOW_RECOMMENDATION_RELIABILITY;
		}
		if (lifecycleUncertaintyDetected) {
			return RecommendationReadinessReason.LIFECYCLE_UNCERTAINTY;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.MEDIUM) {
			return RecommendationReadinessReason.MEDIUM_RECOMMENDATION_RELIABILITY;
		}
		if (ready(
				recommendationReliability,
				lifecycleRisk,
				lifecycleUncertaintyDetected
		)) {
			return RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY;
		}
		return RecommendationReadinessReason.UNKNOWN;
	}

	private RecommendationReadinessScope scope(
			RecommendationReliability recommendationReliability,
			OperationalUncertainty lifecycleRisk,
			boolean lifecycleUncertaintyDetected
	) {
		if (paymentSafetyUncertainty(recommendationReliability)) {
			return RecommendationReadinessScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return RecommendationReadinessScope.LIFECYCLE_RISK;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.BLOCKED
				|| recommendationReliability.level()
				== RecommendationReliabilityLevel.UNRELIABLE) {
			return RecommendationReadinessScope.RECOMMENDATION_RELIABILITY;
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.LOW
				|| recommendationReliability.level()
				== RecommendationReliabilityLevel.MEDIUM) {
			return RecommendationReadinessScope.OPERATOR_VIEW;
		}
		if (lifecycleUncertaintyDetected) {
			return RecommendationReadinessScope.LIFECYCLE_UNCERTAINTY;
		}
		if (ready(
				recommendationReliability,
				lifecycleRisk,
				lifecycleUncertaintyDetected
		)) {
			return RecommendationReadinessScope.RUNTIME_READINESS;
		}
		return RecommendationReadinessScope.RUNTIME_READINESS;
	}

	private boolean ready(
			RecommendationReliability recommendationReliability,
			OperationalUncertainty lifecycleRisk,
			boolean lifecycleUncertaintyDetected
	) {
		return recommendationReliability.level() == RecommendationReliabilityLevel.HIGH
				&& !paymentSafetyUncertainty(recommendationReliability)
				&& lifecycleRisk != OperationalUncertainty.CRITICAL
				&& !lifecycleUncertaintyDetected;
	}

	private boolean paymentSafetyUncertainty(
			RecommendationReliability recommendationReliability
	) {
		return recommendationReliability.reason()
				== RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
	}
}
