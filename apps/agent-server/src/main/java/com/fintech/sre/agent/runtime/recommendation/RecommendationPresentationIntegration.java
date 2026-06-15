package com.fintech.sre.agent.runtime.recommendation;

import java.util.Set;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class RecommendationPresentationIntegration {

	private static final Set<String> VALID_PAYMENT_SAFETY_CLASSIFICATIONS = Set.of(
			"PAYMENT_SAFE_REVIEWED",
			"PAYMENT_RESTRICTED_REVIEWED",
			"PAYMENT_CRITICAL_REVIEWED"
	);

	public RecommendationPresentationIntegrationResult integrate(
			RecommendationPresentation presentation,
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
		if (presentation == null) {
			throw new NullPointerException("presentation must not be null");
		}
		if (lifecycleRisk == null) {
			throw new NullPointerException("lifecycleRisk must not be null");
		}

		if (rawPayloadExposureAttempt) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.RAW_PAYLOAD_PROTECTED,
					RecommendationPresentationIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (vendorDetailExposureAttempt) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.VENDOR_DETAIL_PROTECTED,
					RecommendationPresentationIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (credentialExposureAttempt) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.CREDENTIAL_PROTECTED,
					RecommendationPresentationIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (configurationSecretExposureAttempt) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.CONFIGURATION_SECRET_PROTECTED,
					RecommendationPresentationIntegrationScope.PAYLOAD_PROTECTION,
					false,
					false
			);
		}
		if (!scenarioReferencePresent) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.MISSING_SCENARIO_REFERENCE,
					RecommendationPresentationIntegrationScope.SCENARIO,
					false,
					false
			);
		}
		if (!runbookReferencePresent) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.MISSING_RUNBOOK_REFERENCE,
					RecommendationPresentationIntegrationScope.RUNBOOK,
					false,
					false
			);
		}
		if (!rollbackReferencePresent) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.MISSING_ROLLBACK_REFERENCE,
					RecommendationPresentationIntegrationScope.ROLLBACK,
					false,
					false
			);
		}
		if (!verificationReferencePresent) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.MISSING_VERIFICATION_REFERENCE,
					RecommendationPresentationIntegrationScope.VERIFICATION,
					false,
					false
			);
		}
		if (!evidenceReferencePresent) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.MISSING_EVIDENCE_REFERENCE,
					RecommendationPresentationIntegrationScope.EVIDENCE,
					false,
					false
			);
		}
		if (!paymentSafetyClassificationPresent) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.MISSING_PAYMENT_SAFETY_CLASSIFICATION,
					RecommendationPresentationIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (!VALID_PAYMENT_SAFETY_CLASSIFICATIONS.contains(
				presentation.paymentSafetyClassification()
		)) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.INVALID_PAYMENT_SAFETY_CLASSIFICATION,
					RecommendationPresentationIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					RecommendationPresentationIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (paymentSafetyUncertainty) {
			return result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					RecommendationPresentationIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		return switch (presentation.status()) {
			case PRESENTABLE -> result(
					presentation,
					RecommendationPresentationIntegrationStatus.EXPOSABLE,
					RecommendationPresentationIntegrationReason.VALID_RECOMMENDATION_PRESENTATION,
					RecommendationPresentationIntegrationScope.RECOMMENDATION,
					true,
					true
			);
			case PARTIAL_PRESENTATION -> result(
					presentation,
					RecommendationPresentationIntegrationStatus.PARTIAL,
					RecommendationPresentationIntegrationReason.PARTIAL_PRESENTATION,
					RecommendationPresentationIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_PRESENTABLE -> result(
					presentation,
					RecommendationPresentationIntegrationStatus.NOT_READY,
					RecommendationPresentationIntegrationReason.NOT_PRESENTABLE_RECOMMENDATION,
					RecommendationPresentationIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					presentation,
					RecommendationPresentationIntegrationStatus.UNRELIABLE,
					RecommendationPresentationIntegrationReason.UNRELIABLE_RECOMMENDATION,
					RecommendationPresentationIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					presentation,
					RecommendationPresentationIntegrationStatus.BLOCKED,
					RecommendationPresentationIntegrationReason.BLOCKED_RECOMMENDATION,
					RecommendationPresentationIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					presentation,
					RecommendationPresentationIntegrationStatus.UNKNOWN,
					RecommendationPresentationIntegrationReason.UNKNOWN,
					RecommendationPresentationIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean uiImplementation() {
		return false;
	}

	public boolean restApi() {
		return false;
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

	public boolean operatorFacingExposureReadiness() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private RecommendationPresentationIntegrationResult result(
			RecommendationPresentation presentation,
			RecommendationPresentationIntegrationStatus status,
			RecommendationPresentationIntegrationReason reason,
			RecommendationPresentationIntegrationScope scope,
			boolean operatorFacingPresentationVisible,
			boolean presentationExposureCertaintyAllowed
	) {
		return new RecommendationPresentationIntegrationResult(
				presentation,
				status,
				reason,
				scope,
				operatorFacingPresentationVisible,
				presentationExposureCertaintyAllowed
		);
	}
}
