package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class VerificationReliabilityIntegration {

	public VerificationReliabilityIntegrationResult integrate(
			VerificationReliability verificationReliability
	) {
		Objects.requireNonNull(
				verificationReliability,
				"verificationReliability must not be null"
		);

		EvidenceRuntimeApiResponse apiResponse = verificationReliability
				.approvalReliability()
				.recommendationReliability()
				.decisionReliability()
				.assessmentReliability()
				.evidenceReliability()
				.trustScore()
				.lineageIntegrationResult()
				.apiResponse();

		if (verificationReliability.level() == VerificationReliabilityLevel.BLOCKED) {
			return blockedResult(verificationReliability, apiResponse);
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.UNRELIABLE) {
			return result(
					verificationReliability,
					unreliable(apiResponse),
					VerificationReliabilityIntegrationStatus.UNRELIABLE,
					VerificationReliabilityIntegrationReason.UNRELIABLE_VERIFICATION_RELIABILITY,
					VerificationReliabilityIntegrationScope.VERIFICATION_UNCERTAINTY,
					false,
					false
			);
		}
		if (verificationReliability.reason()
				== VerificationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY) {
			return result(
					verificationReliability,
					paymentCriticalRisk(apiResponse),
					VerificationReliabilityIntegrationStatus.WARNING,
					VerificationReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					VerificationReliabilityIntegrationScope.PAYMENT_CRITICAL_RISK_VIEW,
					false,
					false
			);
		}
		if (verificationReliability.reason() == VerificationReliabilityReason.CONTRADICTORY_APPROVAL
				|| verificationReliability.reason()
				== VerificationReliabilityReason.CONTRADICTORY_RECOMMENDATION) {
			return result(
					verificationReliability,
					lifecycleUncertainty(apiResponse),
					VerificationReliabilityIntegrationStatus.WARNING,
					VerificationReliabilityIntegrationReason.CONTRADICTORY_VERIFICATION,
					VerificationReliabilityIntegrationScope.LIFECYCLE_UNCERTAINTY,
					false,
					false
			);
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.LOW) {
			return result(
					verificationReliability,
					warning(apiResponse),
					VerificationReliabilityIntegrationStatus.WARNING,
					VerificationReliabilityIntegrationReason.LOW_VERIFICATION_RELIABILITY,
					VerificationReliabilityIntegrationScope.OPERATOR_WARNING_VIEW,
					false,
					false
			);
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.MEDIUM) {
			return result(
					verificationReliability,
					partial(apiResponse),
					VerificationReliabilityIntegrationStatus.PARTIAL_VERIFICATION_READINESS,
					VerificationReliabilityIntegrationReason.MEDIUM_VERIFICATION_RELIABILITY,
					VerificationReliabilityIntegrationScope.PARTIAL_VERIFICATION_VIEW,
					false,
					false
			);
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.HIGH) {
			return result(
					verificationReliability,
					apiResponse,
					VerificationReliabilityIntegrationStatus.VERIFICATION_READY,
					VerificationReliabilityIntegrationReason.HIGH_VERIFICATION_RELIABILITY,
					VerificationReliabilityIntegrationScope.VERIFICATION_READY_VIEW,
					true,
					true
			);
		}

		return result(
				verificationReliability,
				unknown(apiResponse),
				VerificationReliabilityIntegrationStatus.UNKNOWN,
				VerificationReliabilityIntegrationReason.UNKNOWN,
				VerificationReliabilityIntegrationScope.UNKNOWN,
				false,
				false
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesVerification() {
		return false;
	}

	public boolean verificationAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private VerificationReliabilityIntegrationResult blockedResult(
			VerificationReliability verificationReliability,
			EvidenceRuntimeApiResponse apiResponse
	) {
		if (verificationReliability.reason()
				== VerificationReliabilityReason.MISSING_VERIFICATION_BINDING) {
			return result(
					verificationReliability,
					lifecycleUncertainty(apiResponse),
					VerificationReliabilityIntegrationStatus.BLOCKED,
					VerificationReliabilityIntegrationReason.MISSING_VERIFICATION_BINDING,
					VerificationReliabilityIntegrationScope.VERIFICATION_BINDING_UNCERTAINTY,
					false,
					false
			);
		}
		if (verificationReliability.reason()
				== VerificationReliabilityReason.MISSING_VERIFICATION_EVIDENCE_REQUIREMENT) {
			return result(
					verificationReliability,
					lifecycleUncertainty(apiResponse),
					VerificationReliabilityIntegrationStatus.BLOCKED,
					VerificationReliabilityIntegrationReason
							.MISSING_VERIFICATION_EVIDENCE_REQUIREMENT,
					VerificationReliabilityIntegrationScope.VERIFICATION_EVIDENCE_UNCERTAINTY,
					false,
					false
			);
		}
		if (verificationReliability.reason()
				== VerificationReliabilityReason.MISSING_ROLLBACK_BINDING) {
			return result(
					verificationReliability,
					lifecycleUncertainty(apiResponse),
					VerificationReliabilityIntegrationStatus.BLOCKED,
					VerificationReliabilityIntegrationReason.MISSING_ROLLBACK_BINDING,
					VerificationReliabilityIntegrationScope.ROLLBACK_UNCERTAINTY,
					false,
					false
			);
		}
		return result(
				verificationReliability,
				blocked(apiResponse),
				VerificationReliabilityIntegrationStatus.BLOCKED,
				VerificationReliabilityIntegrationReason.BLOCKED_VERIFICATION_RELIABILITY,
				VerificationReliabilityIntegrationScope.VERIFICATION_FORBIDDEN,
				false,
				false
		);
	}

	private VerificationReliabilityIntegrationResult result(
			VerificationReliability verificationReliability,
			EvidenceRuntimeApiResponse apiResponse,
			VerificationReliabilityIntegrationStatus status,
			VerificationReliabilityIntegrationReason reason,
			VerificationReliabilityIntegrationScope scope,
			boolean verificationRequestAllowed,
			boolean verificationCertaintyAllowed
	) {
		return new VerificationReliabilityIntegrationResult(
				verificationReliability,
				apiResponse,
				status,
				reason,
				scope,
				verificationRequestAllowed,
				verificationCertaintyAllowed
		);
	}

	private EvidenceRuntimeApiResponse blocked(EvidenceRuntimeApiResponse apiResponse) {
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

	private EvidenceRuntimeApiResponse unreliable(EvidenceRuntimeApiResponse apiResponse) {
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

	private EvidenceRuntimeApiResponse warning(EvidenceRuntimeApiResponse apiResponse) {
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

	private EvidenceRuntimeApiResponse partial(EvidenceRuntimeApiResponse apiResponse) {
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

	private EvidenceRuntimeApiResponse paymentCriticalRisk(
			EvidenceRuntimeApiResponse apiResponse
	) {
		EvidenceRuntimeSummaryView summary = apiResponse.summary();
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.UNCERTAIN,
						OperationalUncertainty.CRITICAL,
						OperationalUncertainty.CRITICAL,
						true,
						summary.uncertaintyReason(),
						summary.auditTrusted(),
						summary.evidenceCompleteness()
				),
				EvidenceRuntimeApiStatus.UNCERTAIN,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}

	private EvidenceRuntimeApiResponse lifecycleUncertainty(
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

	private EvidenceRuntimeApiResponse unknown(EvidenceRuntimeApiResponse apiResponse) {
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
