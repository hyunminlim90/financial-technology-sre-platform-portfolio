package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class AssessmentReliabilityIntegration {

	public AssessmentReliabilityIntegrationResult integrate(
			AssessmentReliability assessmentReliability
	) {
		Objects.requireNonNull(
				assessmentReliability,
				"assessmentReliability must not be null"
		);

		EvidenceRuntimeApiResponse apiResponse = assessmentReliability
				.evidenceReliability()
				.trustScore()
				.lineageIntegrationResult()
				.apiResponse();

		if (assessmentReliability.level() == AssessmentReliabilityLevel.BLOCKED) {
			return result(
					assessmentReliability,
					blocked(apiResponse),
					AssessmentReliabilityIntegrationStatus.BLOCKED,
					AssessmentReliabilityIntegrationReason.BLOCKED_ASSESSMENT_RELIABILITY,
					AssessmentReliabilityIntegrationScope.LIFECYCLE_BLOCKED,
					false,
					false
			);
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.UNRELIABLE) {
			return result(
					assessmentReliability,
					unreliable(apiResponse),
					AssessmentReliabilityIntegrationStatus.UNRELIABLE,
					AssessmentReliabilityIntegrationReason.UNRELIABLE_ASSESSMENT_RELIABILITY,
					AssessmentReliabilityIntegrationScope.RECOMMENDATION_UNCERTAINTY,
					false,
					false
			);
		}
		if (assessmentReliability.reason()
				== AssessmentReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY) {
			return result(
					assessmentReliability,
					paymentRisk(apiResponse),
					AssessmentReliabilityIntegrationStatus.WARNING,
					AssessmentReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					AssessmentReliabilityIntegrationScope.PAYMENT_RISK_VIEW,
					false,
					false
			);
		}
		if (assessmentReliability.reason()
				== AssessmentReliabilityReason.CONTRADICTORY_EVIDENCE) {
			return result(
					assessmentReliability,
					contradictory(apiResponse),
					AssessmentReliabilityIntegrationStatus.WARNING,
					AssessmentReliabilityIntegrationReason.CONTRADICTORY_ASSESSMENT,
					AssessmentReliabilityIntegrationScope.OPERATOR_WARNING_VIEW,
					false,
					false
			);
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.LOW) {
			return result(
					assessmentReliability,
					warning(apiResponse),
					AssessmentReliabilityIntegrationStatus.WARNING,
					AssessmentReliabilityIntegrationReason.LOW_ASSESSMENT_RELIABILITY,
					AssessmentReliabilityIntegrationScope.OPERATOR_WARNING_VIEW,
					false,
					false
			);
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.MEDIUM) {
			return result(
					assessmentReliability,
					partial(apiResponse),
					AssessmentReliabilityIntegrationStatus.PARTIAL_ASSESSMENT_RELIABILITY,
					AssessmentReliabilityIntegrationReason.MEDIUM_ASSESSMENT_RELIABILITY,
					AssessmentReliabilityIntegrationScope.PARTIAL_ASSESSMENT_VIEW,
					false,
					false
			);
		}
		if (assessmentReliability.level() == AssessmentReliabilityLevel.HIGH) {
			return result(
					assessmentReliability,
					apiResponse,
					AssessmentReliabilityIntegrationStatus.RELIABLE,
					AssessmentReliabilityIntegrationReason.HIGH_ASSESSMENT_RELIABILITY,
					AssessmentReliabilityIntegrationScope.RELIABLE_ASSESSMENT_VIEW,
					true,
					true
			);
		}

		return result(
				assessmentReliability,
				unknown(apiResponse),
				AssessmentReliabilityIntegrationStatus.UNKNOWN,
				AssessmentReliabilityIntegrationReason.UNKNOWN,
				AssessmentReliabilityIntegrationScope.UNKNOWN,
				false,
				false
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesAssessment() {
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

	private AssessmentReliabilityIntegrationResult result(
			AssessmentReliability assessmentReliability,
			EvidenceRuntimeApiResponse apiResponse,
			AssessmentReliabilityIntegrationStatus status,
			AssessmentReliabilityIntegrationReason reason,
			AssessmentReliabilityIntegrationScope scope,
			boolean lifecycleStableAllowed,
			boolean recommendationCertaintyAllowed
	) {
		return new AssessmentReliabilityIntegrationResult(
				assessmentReliability,
				apiResponse,
				status,
				reason,
				scope,
				lifecycleStableAllowed,
				recommendationCertaintyAllowed
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

	private EvidenceRuntimeApiResponse contradictory(
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
