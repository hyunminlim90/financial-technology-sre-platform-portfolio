package com.fintech.sre.agent.runtime.recommendation;

import java.time.Instant;
import java.util.Set;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class RecommendationPresentationBuilder {

	private static final Set<String> VALID_PAYMENT_SAFETY_CLASSIFICATIONS = Set.of(
			"PAYMENT_SAFE_REVIEWED",
			"PAYMENT_RESTRICTED_REVIEWED",
			"PAYMENT_CRITICAL_REVIEWED"
	);

	private RecommendationModelIntegrationResult modelIntegrationResult;
	private String scenarioReference;
	private String runbookReference;
	private String rollbackReference;
	private String verificationReference;
	private String evidenceReference;
	private String paymentSafetyClassification;
	private Instant presentationTimestamp;
	private OperationalUncertainty lifecycleRisk = OperationalUncertainty.LOW;
	private boolean paymentSafetyUncertainty;
	private boolean rawPayloadExposureAttempt;
	private boolean vendorDetailExposureAttempt;
	private boolean credentialExposureAttempt;
	private boolean configurationSecretExposureAttempt;

	public RecommendationPresentationBuilder modelIntegrationResult(
			RecommendationModelIntegrationResult modelIntegrationResult
	) {
		this.modelIntegrationResult = modelIntegrationResult;
		return this;
	}

	public RecommendationPresentationBuilder scenarioReference(
			String scenarioReference
	) {
		this.scenarioReference = scenarioReference;
		return this;
	}

	public RecommendationPresentationBuilder runbookReference(String runbookReference) {
		this.runbookReference = runbookReference;
		return this;
	}

	public RecommendationPresentationBuilder rollbackReference(String rollbackReference) {
		this.rollbackReference = rollbackReference;
		return this;
	}

	public RecommendationPresentationBuilder verificationReference(
			String verificationReference
	) {
		this.verificationReference = verificationReference;
		return this;
	}

	public RecommendationPresentationBuilder evidenceReference(String evidenceReference) {
		this.evidenceReference = evidenceReference;
		return this;
	}

	public RecommendationPresentationBuilder paymentSafetyClassification(
			String paymentSafetyClassification
	) {
		this.paymentSafetyClassification = paymentSafetyClassification;
		return this;
	}

	public RecommendationPresentationBuilder presentationTimestamp(
			Instant presentationTimestamp
	) {
		this.presentationTimestamp = presentationTimestamp;
		return this;
	}

	public RecommendationPresentationBuilder lifecycleRisk(
			OperationalUncertainty lifecycleRisk
	) {
		this.lifecycleRisk = lifecycleRisk;
		return this;
	}

	public RecommendationPresentationBuilder paymentSafetyUncertainty(
			boolean paymentSafetyUncertainty
	) {
		this.paymentSafetyUncertainty = paymentSafetyUncertainty;
		return this;
	}

	public RecommendationPresentationBuilder rawPayloadExposureAttempt(
			boolean rawPayloadExposureAttempt
	) {
		this.rawPayloadExposureAttempt = rawPayloadExposureAttempt;
		return this;
	}

	public RecommendationPresentationBuilder vendorDetailExposureAttempt(
			boolean vendorDetailExposureAttempt
	) {
		this.vendorDetailExposureAttempt = vendorDetailExposureAttempt;
		return this;
	}

	public RecommendationPresentationBuilder credentialExposureAttempt(
			boolean credentialExposureAttempt
	) {
		this.credentialExposureAttempt = credentialExposureAttempt;
		return this;
	}

	public RecommendationPresentationBuilder configurationSecretExposureAttempt(
			boolean configurationSecretExposureAttempt
	) {
		this.configurationSecretExposureAttempt = configurationSecretExposureAttempt;
		return this;
	}

	public RecommendationPresentation build() {
		if (modelIntegrationResult == null) {
			throw new NullPointerException(
					"modelIntegrationResult must not be null"
			);
		}
		if (lifecycleRisk == null) {
			throw new NullPointerException("lifecycleRisk must not be null");
		}
		if (rawPayloadExposureAttempt) {
			throw blocked(
					RecommendationPresentationReason.RAW_PAYLOAD_PROTECTED
			);
		}
		if (vendorDetailExposureAttempt) {
			throw blocked(
					RecommendationPresentationReason.VENDOR_DETAIL_PROTECTED
			);
		}
		if (credentialExposureAttempt) {
			throw blocked(
					RecommendationPresentationReason.CREDENTIAL_PROTECTED
			);
		}
		if (configurationSecretExposureAttempt) {
			throw blocked(
					RecommendationPresentationReason.CONFIGURATION_SECRET_PROTECTED
			);
		}
		if (modelIntegrationResult.status()
				!= RecommendationModelIntegrationStatus.RECOMMENDATION_READY) {
			throw blocked(RecommendationPresentationReason.BLOCKED_RECOMMENDATION);
		}
		if (scenarioReference == null || scenarioReference.isBlank()) {
			throw blocked(RecommendationPresentationReason.MISSING_SCENARIO_REFERENCE);
		}
		if (runbookReference == null || runbookReference.isBlank()) {
			throw blocked(RecommendationPresentationReason.MISSING_RUNBOOK_REFERENCE);
		}
		if (rollbackReference == null || rollbackReference.isBlank()) {
			throw blocked(RecommendationPresentationReason.MISSING_ROLLBACK_REFERENCE);
		}
		if (verificationReference == null || verificationReference.isBlank()) {
			throw blocked(
					RecommendationPresentationReason.MISSING_VERIFICATION_REFERENCE
			);
		}
		if (evidenceReference == null || evidenceReference.isBlank()) {
			throw blocked(RecommendationPresentationReason.MISSING_EVIDENCE_REFERENCE);
		}
		if (paymentSafetyClassification == null
				|| paymentSafetyClassification.isBlank()) {
			throw blocked(
					RecommendationPresentationReason
							.MISSING_PAYMENT_SAFETY_CLASSIFICATION
			);
		}
		if (!VALID_PAYMENT_SAFETY_CLASSIFICATIONS.contains(
				paymentSafetyClassification
		)) {
			throw blocked(
					RecommendationPresentationReason
							.INVALID_PAYMENT_SAFETY_CLASSIFICATION
			);
		}
		if (paymentSafetyUncertainty) {
			throw blocked(
					RecommendationPresentationReason.PAYMENT_SAFETY_UNCERTAINTY
			);
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			throw blocked(
					RecommendationPresentationReason.CRITICAL_LIFECYCLE_RISK
			);
		}

		RecommendationModel model = modelIntegrationResult.model();
		return new RecommendationPresentation(
				model.recommendationId(),
				model.title(),
				model.summary(),
				model.recommendationType(),
				model.recommendationReason(),
				scenarioReference,
				runbookReference,
				rollbackReference,
				verificationReference,
				evidenceReference,
				paymentSafetyClassification,
				presentationTimestamp,
				RecommendationPresentationStatus.PRESENTABLE,
				RecommendationPresentationReason.VALID_RECOMMENDATION,
				RecommendationPresentationScope.PRESENTATION
		);
	}

	private IllegalStateException blocked(RecommendationPresentationReason reason) {
		return new IllegalStateException("presentation blocked: " + reason.name());
	}
}
