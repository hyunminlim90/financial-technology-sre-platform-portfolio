package com.fintech.sre.agent.runtime.recommendation;

import java.util.Set;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class RecommendationModelIntegration {

	private static final Set<String> VALID_PAYMENT_SAFETY_CLASSIFICATIONS = Set.of(
			"PAYMENT_SAFE_REVIEWED",
			"PAYMENT_RESTRICTED_REVIEWED",
			"PAYMENT_CRITICAL_REVIEWED"
	);

	public RecommendationModelIntegrationResult integrate(
			RecommendationModel model,
			boolean scenarioReferencePresent,
			boolean runbookReferencePresent,
			boolean rollbackReferencePresent,
			boolean verificationReferencePresent,
			boolean evidenceReferencePresent,
			boolean paymentSafetyClassificationPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty,
			boolean rawPayloadExposureAttempt,
			boolean vendorDetailExposureAttempt,
			boolean credentialExposureAttempt,
			boolean configurationSecretExposureAttempt
	) {
		if (model == null) {
			throw new NullPointerException("model must not be null");
		}
		if (lifecycleRisk == null) {
			throw new NullPointerException("lifecycleRisk must not be null");
		}

		if (rawPayloadExposureAttempt) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.RAW_PAYLOAD_PROTECTED,
					RecommendationModelIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (vendorDetailExposureAttempt) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.VENDOR_DETAIL_PROTECTED,
					RecommendationModelIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (credentialExposureAttempt) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.CREDENTIAL_PROTECTED,
					RecommendationModelIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (configurationSecretExposureAttempt) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.CONFIGURATION_SECRET_PROTECTED,
					RecommendationModelIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (!scenarioReferencePresent) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.MISSING_SCENARIO_REFERENCE,
					RecommendationModelIntegrationScope.SCENARIO,
					false,
					false
			);
		}
		if (!runbookReferencePresent) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.MISSING_RUNBOOK_REFERENCE,
					RecommendationModelIntegrationScope.RUNBOOK,
					false,
					false
			);
		}
		if (!rollbackReferencePresent) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.MISSING_ROLLBACK_REFERENCE,
					RecommendationModelIntegrationScope.ROLLBACK,
					false,
					false
			);
		}
		if (!verificationReferencePresent) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.MISSING_VERIFICATION_REFERENCE,
					RecommendationModelIntegrationScope.VERIFICATION,
					false,
					false
			);
		}
		if (!evidenceReferencePresent) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.MISSING_EVIDENCE_REFERENCE,
					RecommendationModelIntegrationScope.EVIDENCE,
					false,
					false
			);
		}
		if (!paymentSafetyClassificationPresent) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason
							.MISSING_PAYMENT_SAFETY_CLASSIFICATION,
					RecommendationModelIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (!VALID_PAYMENT_SAFETY_CLASSIFICATIONS.contains(
				model.paymentSafetyClassification()
		)) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason
							.INVALID_PAYMENT_SAFETY_CLASSIFICATION,
					RecommendationModelIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					RecommendationModelIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (paymentSafetyUncertainty) {
			return result(
					model,
					RecommendationModelIntegrationStatus.BLOCKED,
					RecommendationModelIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					RecommendationModelIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (model.recommendationType() == RecommendationModelType.UNKNOWN
				|| model.recommendationReason() == RecommendationModelReason.UNKNOWN) {
			return result(
					model,
					RecommendationModelIntegrationStatus.PARTIAL_RECOMMENDATION,
					RecommendationModelIntegrationReason.PARTIAL_RECOMMENDATION_MODEL,
					RecommendationModelIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		}
		return result(
				model,
				RecommendationModelIntegrationStatus.RECOMMENDATION_READY,
				RecommendationModelIntegrationReason.VALID_RECOMMENDATION_MODEL,
				RecommendationModelIntegrationScope.RECOMMENDATION_MODEL,
				true,
				true
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendationMutation() {
		return false;
	}

	public boolean approvalRequest() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean operatorFacingRecommendationExposureReadiness() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private RecommendationModelIntegrationResult result(
			RecommendationModel model,
			RecommendationModelIntegrationStatus status,
			RecommendationModelIntegrationReason reason,
			RecommendationModelIntegrationScope scope,
			boolean operatorFacingRecommendationVisible,
			boolean recommendationExposureCertaintyAllowed
	) {
		return new RecommendationModelIntegrationResult(
				model,
				status,
				reason,
				scope,
				operatorFacingRecommendationVisible,
				recommendationExposureCertaintyAllowed
		);
	}
}
