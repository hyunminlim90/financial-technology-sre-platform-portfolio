package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceTrustIntegration {

	public EvidenceTrustIntegrationResult integrate(
			EvidenceTrustScore trustScore
	) {
		Objects.requireNonNull(trustScore, "trustScore must not be null");

		if (trustScore.level() == EvidenceTrustScoreLevel.UNTRUSTED) {
			return result(
					trustScore,
					blocked(trustScore.lineageIntegrationResult().apiResponse()),
					EvidenceTrustIntegrationStatus.UNTRUSTED,
					trustScore.reason() == EvidenceTrustScoreReason.BLOCKED_EVIDENCE
							? EvidenceTrustIntegrationReason.BLOCKED_EVIDENCE
							: EvidenceTrustIntegrationReason.UNTRUSTED_SCORE,
					EvidenceTrustIntegrationScope.API_BLOCKED
			);
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.LOW) {
			return result(
					trustScore,
					warning(trustScore.lineageIntegrationResult().apiResponse()),
					EvidenceTrustIntegrationStatus.WARNING,
					EvidenceTrustIntegrationReason.LOW_TRUST_SCORE,
					EvidenceTrustIntegrationScope.OPERATOR_WARNING_VIEW
			);
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.MEDIUM) {
			return result(
					trustScore,
					partial(trustScore.lineageIntegrationResult().apiResponse()),
					EvidenceTrustIntegrationStatus.PARTIAL_TRUST,
					trustScore.reason() == EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE
							? EvidenceTrustIntegrationReason.PAYMENT_TRUST_RESTRICTED
							: EvidenceTrustIntegrationReason.MEDIUM_TRUST_SCORE,
					trustScore.scope() == EvidenceTrustScoreScope.PAYMENT_EVIDENCE
							? EvidenceTrustIntegrationScope.PAYMENT_RESTRICTED_VIEW
							: EvidenceTrustIntegrationScope.PARTIAL_TRUST_VIEW
			);
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.HIGH) {
			return result(
					trustScore,
					trustScore.lineageIntegrationResult().apiResponse(),
					EvidenceTrustIntegrationStatus.TRUSTED,
					EvidenceTrustIntegrationReason.HIGH_TRUST_SCORE,
					EvidenceTrustIntegrationScope.TRUSTED_EVIDENCE_VIEW
			);
		}

		return result(
				trustScore,
				unknown(trustScore.lineageIntegrationResult().apiResponse()),
				EvidenceTrustIntegrationStatus.UNKNOWN,
				EvidenceTrustIntegrationReason.UNKNOWN,
				EvidenceTrustIntegrationScope.UNKNOWN
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

	private EvidenceTrustIntegrationResult result(
			EvidenceTrustScore trustScore,
			EvidenceRuntimeApiResponse apiResponse,
			EvidenceTrustIntegrationStatus status,
			EvidenceTrustIntegrationReason reason,
			EvidenceTrustIntegrationScope scope
	) {
		return new EvidenceTrustIntegrationResult(
				trustScore,
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
						false,
						summary.evidenceCompleteness()
				),
				EvidenceRuntimeApiStatus.UNCERTAIN,
				EvidenceRuntimeApiRejectionReason.UNTRUSTED_AUDIT
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
