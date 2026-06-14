package com.fintech.sre.agent.runtime.recommendation;

import java.util.Set;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class RecommendationGenerationEvaluator {

	private static final Set<String> VALID_PAYMENT_SAFETY_CLASSIFICATIONS = Set.of(
			"PAYMENT_SAFE_REVIEWED",
			"PAYMENT_RESTRICTED_REVIEWED",
			"PAYMENT_CRITICAL_REVIEWED"
	);

	public RecommendationGeneration evaluate(
			RecommendationContentIntegrationResult contentIntegrationResult,
			boolean scenarioIdPresent,
			boolean runbookIdPresent,
			boolean rollbackIdPresent,
			boolean verificationIdPresent,
			boolean paymentSafetyClassificationPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (contentIntegrationResult == null) {
			throw new NullPointerException(
					"contentIntegrationResult must not be null"
			);
		}
		if (lifecycleRisk == null) {
			throw new NullPointerException("lifecycleRisk must not be null");
		}

		return new RecommendationGeneration(
				level(
						contentIntegrationResult,
						scenarioIdPresent,
						runbookIdPresent,
						rollbackIdPresent,
						verificationIdPresent,
						paymentSafetyClassificationPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						contentIntegrationResult,
						scenarioIdPresent,
						runbookIdPresent,
						rollbackIdPresent,
						verificationIdPresent,
						paymentSafetyClassificationPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						contentIntegrationResult,
						scenarioIdPresent,
						runbookIdPresent,
						rollbackIdPresent,
						verificationIdPresent,
						paymentSafetyClassificationPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				contentIntegrationResult
		);
	}

	private RecommendationGenerationLevel level(
			RecommendationContentIntegrationResult contentIntegrationResult,
			boolean scenarioIdPresent,
			boolean runbookIdPresent,
			boolean rollbackIdPresent,
			boolean verificationIdPresent,
			boolean paymentSafetyClassificationPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (missingScenarioId(scenarioIdPresent)) {
			return RecommendationGenerationLevel.BLOCKED;
		}
		if (missingRunbookId(runbookIdPresent)) {
			return RecommendationGenerationLevel.BLOCKED;
		}
		if (missingRollbackId(rollbackIdPresent)) {
			return RecommendationGenerationLevel.BLOCKED;
		}
		if (missingVerificationId(verificationIdPresent)) {
			return RecommendationGenerationLevel.BLOCKED;
		}
		if (missingPaymentSafetyClassification(paymentSafetyClassificationPresent)) {
			return RecommendationGenerationLevel.BLOCKED;
		}
		if (invalidPaymentSafetyClassification(contentIntegrationResult)) {
			return RecommendationGenerationLevel.BLOCKED;
		}
		if (criticalLifecycleRisk(lifecycleRisk)) {
			return RecommendationGenerationLevel.BLOCKED;
		}
		if (paymentSafetyUncertainty(paymentSafetyUncertainty)) {
			return RecommendationGenerationLevel.BLOCKED;
		}
		return switch (contentIntegrationResult.status()) {
			case CONTENT_READY -> RecommendationGenerationLevel.GENERATABLE;
			case PARTIAL_CONTENT -> RecommendationGenerationLevel.PARTIAL;
			case NOT_READY -> RecommendationGenerationLevel.NOT_READY;
			case UNRELIABLE -> RecommendationGenerationLevel.UNRELIABLE;
			case BLOCKED -> RecommendationGenerationLevel.BLOCKED;
			case UNKNOWN -> RecommendationGenerationLevel.UNKNOWN;
		};
	}

	private RecommendationGenerationReason reason(
			RecommendationContentIntegrationResult contentIntegrationResult,
			boolean scenarioIdPresent,
			boolean runbookIdPresent,
			boolean rollbackIdPresent,
			boolean verificationIdPresent,
			boolean paymentSafetyClassificationPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (missingScenarioId(scenarioIdPresent)) {
			return RecommendationGenerationReason.MISSING_SCENARIO_ID;
		}
		if (missingRunbookId(runbookIdPresent)) {
			return RecommendationGenerationReason.MISSING_RUNBOOK_ID;
		}
		if (missingRollbackId(rollbackIdPresent)) {
			return RecommendationGenerationReason.MISSING_ROLLBACK_ID;
		}
		if (missingVerificationId(verificationIdPresent)) {
			return RecommendationGenerationReason.MISSING_VERIFICATION_ID;
		}
		if (missingPaymentSafetyClassification(paymentSafetyClassificationPresent)) {
			return RecommendationGenerationReason.MISSING_PAYMENT_SAFETY_CLASSIFICATION;
		}
		if (invalidPaymentSafetyClassification(contentIntegrationResult)) {
			return RecommendationGenerationReason.INVALID_PAYMENT_SAFETY_CLASSIFICATION;
		}
		if (criticalLifecycleRisk(lifecycleRisk)) {
			return RecommendationGenerationReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (paymentSafetyUncertainty(paymentSafetyUncertainty)) {
			return RecommendationGenerationReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		return switch (contentIntegrationResult.status()) {
			case CONTENT_READY -> RecommendationGenerationReason.CONTENT_READY;
			case PARTIAL_CONTENT -> RecommendationGenerationReason.PARTIAL_CONTENT;
			case NOT_READY -> RecommendationGenerationReason.NOT_READY_CONTENT;
			case UNRELIABLE -> RecommendationGenerationReason.UNRELIABLE_CONTENT;
			case BLOCKED -> RecommendationGenerationReason.BLOCKED_CONTENT;
			case UNKNOWN -> RecommendationGenerationReason.UNKNOWN;
		};
	}

	private RecommendationGenerationScope scope(
			RecommendationContentIntegrationResult contentIntegrationResult,
			boolean scenarioIdPresent,
			boolean runbookIdPresent,
			boolean rollbackIdPresent,
			boolean verificationIdPresent,
			boolean paymentSafetyClassificationPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (missingScenarioId(scenarioIdPresent)) {
			return RecommendationGenerationScope.SCENARIO;
		}
		if (missingRunbookId(runbookIdPresent)) {
			return RecommendationGenerationScope.RUNBOOK;
		}
		if (missingRollbackId(rollbackIdPresent)) {
			return RecommendationGenerationScope.ROLLBACK;
		}
		if (missingVerificationId(verificationIdPresent)) {
			return RecommendationGenerationScope.VERIFICATION;
		}
		if (missingPaymentSafetyClassification(paymentSafetyClassificationPresent)
				|| invalidPaymentSafetyClassification(contentIntegrationResult)
				|| paymentSafetyUncertainty(paymentSafetyUncertainty)) {
			return RecommendationGenerationScope.PAYMENT_SAFETY;
		}
		if (criticalLifecycleRisk(lifecycleRisk)) {
			return RecommendationGenerationScope.LIFECYCLE_RISK;
		}
		if (contentIntegrationResult.status()
				== RecommendationContentIntegrationStatus.CONTENT_READY) {
			return RecommendationGenerationScope.RECOMMENDATION_GENERATION;
		}
		return RecommendationGenerationScope.RECOMMENDATION_CONTENT;
	}

	private boolean missingScenarioId(boolean scenarioIdPresent) {
		return !scenarioIdPresent;
	}

	private boolean missingRunbookId(boolean runbookIdPresent) {
		return !runbookIdPresent;
	}

	private boolean missingRollbackId(boolean rollbackIdPresent) {
		return !rollbackIdPresent;
	}

	private boolean missingVerificationId(boolean verificationIdPresent) {
		return !verificationIdPresent;
	}

	private boolean missingPaymentSafetyClassification(
			boolean paymentSafetyClassificationPresent
	) {
		return !paymentSafetyClassificationPresent;
	}

	private boolean invalidPaymentSafetyClassification(
			RecommendationContentIntegrationResult contentIntegrationResult
	) {
		return !VALID_PAYMENT_SAFETY_CLASSIFICATIONS.contains(
				contentIntegrationResult.content().paymentSafetyClassification()
		);
	}

	private boolean criticalLifecycleRisk(OperationalUncertainty lifecycleRisk) {
		return lifecycleRisk == OperationalUncertainty.CRITICAL;
	}

	private boolean paymentSafetyUncertainty(boolean paymentSafetyUncertainty) {
		return paymentSafetyUncertainty;
	}
}
