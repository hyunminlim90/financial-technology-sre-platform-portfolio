package com.fintech.sre.agent.runtime.recommendation;

import java.util.Set;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class RecommendationContentIntegration {

	private static final Set<String> VALID_PAYMENT_SAFETY_CLASSIFICATIONS = Set.of(
			"PAYMENT_SAFE_REVIEWED",
			"PAYMENT_RESTRICTED_REVIEWED",
			"PAYMENT_CRITICAL_REVIEWED"
	);

	public RecommendationContentIntegrationResult integrate(
			RecommendationContent content,
			boolean scenarioBindingPresent,
			boolean runbookBindingPresent,
			boolean rollbackBindingPresent,
			boolean verificationBindingPresent,
			boolean paymentSafetyClassificationPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty,
			boolean rawPayloadExposureAttempt,
			boolean vendorDetailExposureAttempt,
			boolean credentialExposureAttempt,
			boolean configurationSecretExposureAttempt
	) {
		if (content == null) {
			throw new NullPointerException("content must not be null");
		}
		if (lifecycleRisk == null) {
			throw new NullPointerException("lifecycleRisk must not be null");
		}

		if (rawPayloadExposureAttempt) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason.RAW_PAYLOAD_PROTECTED,
					RecommendationContentIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (vendorDetailExposureAttempt) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason.VENDOR_DETAIL_PROTECTED,
					RecommendationContentIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (credentialExposureAttempt) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason.CREDENTIAL_PROTECTED,
					RecommendationContentIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (configurationSecretExposureAttempt) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason
							.CONFIGURATION_SECRET_PROTECTED,
					RecommendationContentIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (!scenarioBindingPresent) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason.MISSING_SCENARIO_BINDING,
					RecommendationContentIntegrationScope.SCENARIO,
					false,
					false
			);
		}
		if (!runbookBindingPresent) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason.MISSING_RUNBOOK_BINDING,
					RecommendationContentIntegrationScope.RUNBOOK,
					false,
					false
			);
		}
		if (!rollbackBindingPresent) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason.MISSING_ROLLBACK_BINDING,
					RecommendationContentIntegrationScope.ROLLBACK,
					false,
					false
			);
		}
		if (!verificationBindingPresent) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason.MISSING_VERIFICATION_BINDING,
					RecommendationContentIntegrationScope.VERIFICATION,
					false,
					false
			);
		}
		if (!paymentSafetyClassificationPresent) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason
							.MISSING_PAYMENT_SAFETY_CLASSIFICATION,
					RecommendationContentIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (!validPaymentSafetyClassification(content.paymentSafetyClassification())) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason
							.INVALID_PAYMENT_SAFETY_CLASSIFICATION,
					RecommendationContentIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					RecommendationContentIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (paymentSafetyUncertainty) {
			return result(
					content,
					RecommendationContentIntegrationStatus.BLOCKED,
					RecommendationContentIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					RecommendationContentIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (content.reason() == RecommendationContentReason.UNKNOWN
				|| content.recommendationType() == RecommendationContentType.UNKNOWN) {
			return result(
					content,
					RecommendationContentIntegrationStatus.PARTIAL_CONTENT,
					RecommendationContentIntegrationReason.PARTIAL_CONTENT,
					RecommendationContentIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		}
		return result(
				content,
				RecommendationContentIntegrationStatus.CONTENT_READY,
				RecommendationContentIntegrationReason.VALID_CONTENT,
				RecommendationContentIntegrationScope.RECOMMENDATION_CONTENT,
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

	public boolean operatorFacingContentExposureReadiness() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean validPaymentSafetyClassification(
			String paymentSafetyClassification
	) {
		return VALID_PAYMENT_SAFETY_CLASSIFICATIONS.contains(
				paymentSafetyClassification
		);
	}

	private RecommendationContentIntegrationResult result(
			RecommendationContent content,
			RecommendationContentIntegrationStatus status,
			RecommendationContentIntegrationReason reason,
			RecommendationContentIntegrationScope scope,
			boolean operatorFacingContentVisible,
			boolean contentExposureCertaintyAllowed
	) {
		return new RecommendationContentIntegrationResult(
				content,
				status,
				reason,
				scope,
				operatorFacingContentVisible,
				contentExposureCertaintyAllowed
		);
	}
}
