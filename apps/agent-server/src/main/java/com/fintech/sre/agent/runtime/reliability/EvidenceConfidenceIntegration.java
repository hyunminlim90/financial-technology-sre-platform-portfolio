package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceConfidenceIntegration {

	public EvidenceConfidenceIntegrationResult integrate(
			EvidenceConfidence confidence
	) {
		Objects.requireNonNull(confidence, "confidence must not be null");

		if (confidence.level() == EvidenceConfidenceLevel.INSUFFICIENT) {
			return result(
					confidence,
					insufficient(confidence.trustScore().lineageIntegrationResult().apiResponse()),
					EvidenceConfidenceIntegrationStatus.INSUFFICIENT,
					confidence.reason() == EvidenceConfidenceReason.PAYMENT_EVIDENCE_MISSING
							? EvidenceConfidenceIntegrationReason.PAYMENT_CONFIDENCE_DOWNGRADE
							: EvidenceConfidenceIntegrationReason.INSUFFICIENT_CONFIDENCE,
					confidence.scope() == EvidenceConfidenceScope.PAYMENT_EVIDENCE
							? EvidenceConfidenceIntegrationScope.PAYMENT_UNCERTAINTY_VIEW
							: EvidenceConfidenceIntegrationScope.ASSESSMENT_UNCERTAINTY
			);
		}
		if (confidence.level() == EvidenceConfidenceLevel.LOW) {
			return result(
					confidence,
					warning(confidence.trustScore().lineageIntegrationResult().apiResponse()),
					EvidenceConfidenceIntegrationStatus.WARNING,
					confidence.reason() == EvidenceConfidenceReason.CONTRADICTORY_EVIDENCE
							? EvidenceConfidenceIntegrationReason.CONTRADICTORY_CONFIDENCE
							: EvidenceConfidenceIntegrationReason.LOW_CONFIDENCE,
					EvidenceConfidenceIntegrationScope.OPERATOR_WARNING_VIEW
			);
		}
		if (confidence.level() == EvidenceConfidenceLevel.MEDIUM) {
			return result(
					confidence,
					partial(confidence.trustScore().lineageIntegrationResult().apiResponse()),
					EvidenceConfidenceIntegrationStatus.PARTIAL_CONFIDENCE,
					EvidenceConfidenceIntegrationReason.MEDIUM_CONFIDENCE,
					EvidenceConfidenceIntegrationScope.PARTIAL_CONFIDENCE_VIEW
			);
		}
		if (confidence.level() == EvidenceConfidenceLevel.HIGH) {
			return result(
					confidence,
					confidence.trustScore().lineageIntegrationResult().apiResponse(),
					EvidenceConfidenceIntegrationStatus.CONFIDENT,
					EvidenceConfidenceIntegrationReason.HIGH_CONFIDENCE,
					EvidenceConfidenceIntegrationScope.CONFIDENT_EVIDENCE_VIEW
			);
		}

		return result(
				confidence,
				unknown(confidence.trustScore().lineageIntegrationResult().apiResponse()),
				EvidenceConfidenceIntegrationStatus.UNKNOWN,
				EvidenceConfidenceIntegrationReason.UNKNOWN,
				EvidenceConfidenceIntegrationScope.UNKNOWN
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesEvidence() {
		return false;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private EvidenceConfidenceIntegrationResult result(
			EvidenceConfidence confidence,
			EvidenceRuntimeApiResponse apiResponse,
			EvidenceConfidenceIntegrationStatus status,
			EvidenceConfidenceIntegrationReason reason,
			EvidenceConfidenceIntegrationScope scope
	) {
		return new EvidenceConfidenceIntegrationResult(
				confidence,
				apiResponse,
				status,
				reason,
				scope
		);
	}

	private EvidenceRuntimeApiResponse insufficient(
			EvidenceRuntimeApiResponse apiResponse
	) {
		EvidenceRuntimeSummaryView summary = apiResponse.summary();
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.UNCERTAIN,
						OperationalUncertainty.HIGH,
						OperationalUncertainty.HIGH,
						true,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						false,
						summary.evidenceCompleteness()
				),
				EvidenceRuntimeApiStatus.UNTRUSTED,
				EvidenceRuntimeApiRejectionReason.UNTRUSTED_AUDIT
		);
	}

	private EvidenceRuntimeApiResponse warning(
			EvidenceRuntimeApiResponse apiResponse
	) {
		EvidenceRuntimeSummaryView summary = apiResponse.summary();
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.UNCERTAIN,
						OperationalUncertainty.HIGH,
						summary.paymentSafetyState(),
						true,
						summary.uncertaintyReason(),
						summary.auditTrusted(),
						summary.evidenceCompleteness()
				),
				EvidenceRuntimeApiStatus.UNCERTAIN,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}

	private EvidenceRuntimeApiResponse partial(
			EvidenceRuntimeApiResponse apiResponse
	) {
		EvidenceRuntimeSummaryView summary = apiResponse.summary();
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.UNCERTAIN,
						OperationalUncertainty.MODERATE,
						summary.paymentSafetyState(),
						true,
						summary.uncertaintyReason(),
						summary.auditTrusted(),
						summary.evidenceCompleteness()
				),
				EvidenceRuntimeApiStatus.UNCERTAIN,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}

	private EvidenceRuntimeApiResponse unknown(
			EvidenceRuntimeApiResponse apiResponse
	) {
		EvidenceRuntimeSummaryView summary = apiResponse.summary();
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.UNKNOWN,
						OperationalUncertainty.MODERATE,
						summary.paymentSafetyState(),
						true,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						summary.auditTrusted(),
						summary.evidenceCompleteness()
				),
				EvidenceRuntimeApiStatus.UNKNOWN,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}
}
