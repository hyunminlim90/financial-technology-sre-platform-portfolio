package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceReliabilityIntegration {

	public EvidenceReliabilityIntegrationResult integrate(
			EvidenceReliability reliability
	) {
		Objects.requireNonNull(reliability, "reliability must not be null");

		if (reliability.level() == EvidenceReliabilityLevel.BLOCKED) {
			return result(
					reliability,
					blocked(reliability.trustScore().lineageIntegrationResult().apiResponse()),
					EvidenceReliabilityIntegrationStatus.BLOCKED,
					EvidenceReliabilityIntegrationReason.BLOCKED_RELIABILITY,
					EvidenceReliabilityIntegrationScope.API_BLOCKED
			);
		}
		if (reliability.level() == EvidenceReliabilityLevel.UNRELIABLE) {
			return result(
					reliability,
					unreliable(reliability.trustScore().lineageIntegrationResult().apiResponse()),
					EvidenceReliabilityIntegrationStatus.UNRELIABLE,
					EvidenceReliabilityIntegrationReason.UNRELIABLE_EVIDENCE,
					EvidenceReliabilityIntegrationScope.ASSESSMENT_UNCERTAINTY
			);
		}
		if (reliability.paymentSafetyUncertainty()) {
			return result(
					reliability,
					paymentRisk(reliability.trustScore().lineageIntegrationResult().apiResponse()),
					EvidenceReliabilityIntegrationStatus.RESTRICTED,
					EvidenceReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					EvidenceReliabilityIntegrationScope.PAYMENT_RISK_VIEW
			);
		}
		if (reliability.level() == EvidenceReliabilityLevel.RESTRICTED) {
			return result(
					reliability,
					partial(reliability.trustScore().lineageIntegrationResult().apiResponse()),
					EvidenceReliabilityIntegrationStatus.RESTRICTED,
					EvidenceReliabilityIntegrationReason.RESTRICTED_RELIABILITY,
					EvidenceReliabilityIntegrationScope.PARTIAL_RELIABILITY_VIEW
			);
		}
		if (reliability.level() == EvidenceReliabilityLevel.LOW) {
			return result(
					reliability,
					warning(reliability.trustScore().lineageIntegrationResult().apiResponse()),
					EvidenceReliabilityIntegrationStatus.WARNING,
					EvidenceReliabilityIntegrationReason.LOW_RELIABILITY,
					EvidenceReliabilityIntegrationScope.OPERATOR_WARNING_VIEW
			);
		}
		if (reliability.level() == EvidenceReliabilityLevel.MEDIUM) {
			return result(
					reliability,
					partial(reliability.trustScore().lineageIntegrationResult().apiResponse()),
					EvidenceReliabilityIntegrationStatus.PARTIAL_RELIABILITY,
					EvidenceReliabilityIntegrationReason.MEDIUM_RELIABILITY,
					EvidenceReliabilityIntegrationScope.PARTIAL_RELIABILITY_VIEW
			);
		}
		if (reliability.level() == EvidenceReliabilityLevel.HIGH) {
			return result(
					reliability,
					reliability.trustScore().lineageIntegrationResult().apiResponse(),
					EvidenceReliabilityIntegrationStatus.TRUSTED,
					EvidenceReliabilityIntegrationReason.HIGH_RELIABILITY,
					EvidenceReliabilityIntegrationScope.TRUSTED_EVIDENCE_VIEW
			);
		}

		return result(
				reliability,
				unknown(reliability.trustScore().lineageIntegrationResult().apiResponse()),
				EvidenceReliabilityIntegrationStatus.UNKNOWN,
				EvidenceReliabilityIntegrationReason.UNKNOWN,
				EvidenceReliabilityIntegrationScope.UNKNOWN
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

	private EvidenceReliabilityIntegrationResult result(
			EvidenceReliability reliability,
			EvidenceRuntimeApiResponse apiResponse,
			EvidenceReliabilityIntegrationStatus status,
			EvidenceReliabilityIntegrationReason reason,
			EvidenceReliabilityIntegrationScope scope
	) {
		return new EvidenceReliabilityIntegrationResult(
				reliability,
				apiResponse,
				status,
				reason,
				scope
		);
	}

	private EvidenceRuntimeApiResponse blocked(
			EvidenceRuntimeApiResponse apiResponse
	) {
		EvidenceRuntimeSummaryView summary = apiResponse.summary();
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.UNKNOWN,
						OperationalUncertainty.CRITICAL,
						summary.paymentSafetyState(),
						true,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						false,
						summary.evidenceCompleteness()
				),
				EvidenceRuntimeApiStatus.REJECTED,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}

	private EvidenceRuntimeApiResponse unreliable(
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

	private EvidenceRuntimeApiResponse paymentRisk(
			EvidenceRuntimeApiResponse apiResponse
	) {
		EvidenceRuntimeSummaryView summary = apiResponse.summary();
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.UNCERTAIN,
						OperationalUncertainty.HIGH,
						OperationalUncertainty.HIGH,
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
