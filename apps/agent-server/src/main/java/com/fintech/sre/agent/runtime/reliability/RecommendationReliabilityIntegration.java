package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class RecommendationReliabilityIntegration {

	public RecommendationReliabilityIntegrationResult integrate(
			RecommendationReliability recommendationReliability
	) {
		Objects.requireNonNull(
				recommendationReliability,
				"recommendationReliability must not be null"
		);

		EvidenceRuntimeApiResponse apiResponse = recommendationReliability
				.decisionReliability()
				.assessmentReliability()
				.evidenceReliability()
				.trustScore()
				.lineageIntegrationResult()
				.apiResponse();

		if (recommendationReliability.level() == RecommendationReliabilityLevel.BLOCKED) {
			return blockedResult(recommendationReliability, apiResponse);
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.UNRELIABLE) {
			return result(
					recommendationReliability,
					unreliable(apiResponse),
					RecommendationReliabilityIntegrationStatus.UNRELIABLE,
					RecommendationReliabilityIntegrationReason
							.UNRELIABLE_RECOMMENDATION_RELIABILITY,
					RecommendationReliabilityIntegrationScope.RECOMMENDATION_UNCERTAINTY,
					false,
					false
			);
		}
		if (recommendationReliability.reason()
				== RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY) {
			return result(
					recommendationReliability,
					paymentCriticalRisk(apiResponse),
					RecommendationReliabilityIntegrationStatus.WARNING,
					RecommendationReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					RecommendationReliabilityIntegrationScope.PAYMENT_CRITICAL_RISK_VIEW,
					false,
					false
			);
		}
		if (recommendationReliability.reason()
				== RecommendationReliabilityReason.CONTRADICTORY_DECISION) {
			return result(
					recommendationReliability,
					lifecycleUncertainty(apiResponse),
					RecommendationReliabilityIntegrationStatus.WARNING,
					RecommendationReliabilityIntegrationReason.CONTRADICTORY_RECOMMENDATION,
					RecommendationReliabilityIntegrationScope.LIFECYCLE_UNCERTAINTY,
					false,
					false
			);
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.LOW) {
			return result(
					recommendationReliability,
					warning(apiResponse),
					RecommendationReliabilityIntegrationStatus.WARNING,
					RecommendationReliabilityIntegrationReason.LOW_RECOMMENDATION_RELIABILITY,
					RecommendationReliabilityIntegrationScope.OPERATOR_WARNING_VIEW,
					false,
					false
			);
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.MEDIUM) {
			return result(
					recommendationReliability,
					partial(apiResponse),
					RecommendationReliabilityIntegrationStatus
							.PARTIAL_RECOMMENDATION_RELIABILITY,
					RecommendationReliabilityIntegrationReason
							.MEDIUM_RECOMMENDATION_RELIABILITY,
					RecommendationReliabilityIntegrationScope.PARTIAL_RECOMMENDATION_VIEW,
					false,
					false
			);
		}
		if (recommendationReliability.level() == RecommendationReliabilityLevel.HIGH) {
			return result(
					recommendationReliability,
					apiResponse,
					RecommendationReliabilityIntegrationStatus.RELIABLE,
					RecommendationReliabilityIntegrationReason
							.HIGH_RECOMMENDATION_RELIABILITY,
					RecommendationReliabilityIntegrationScope.RELIABLE_RECOMMENDATION_VIEW,
					true,
					true
			);
		}

		return result(
				recommendationReliability,
				unknown(apiResponse),
				RecommendationReliabilityIntegrationStatus.UNKNOWN,
				RecommendationReliabilityIntegrationReason.UNKNOWN,
				RecommendationReliabilityIntegrationScope.UNKNOWN,
				false,
				false
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesRecommendation() {
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

	private RecommendationReliabilityIntegrationResult blockedResult(
			RecommendationReliability recommendationReliability,
			EvidenceRuntimeApiResponse apiResponse
	) {
		if (recommendationReliability.reason()
				== RecommendationReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT) {
			return result(
					recommendationReliability,
					lifecycleUncertainty(apiResponse),
					RecommendationReliabilityIntegrationStatus.BLOCKED,
					RecommendationReliabilityIntegrationReason
							.MISSING_HUMAN_APPROVAL_REQUIREMENT,
					RecommendationReliabilityIntegrationScope.HUMAN_APPROVAL_UNCERTAINTY,
					false,
					false
			);
		}
		if (recommendationReliability.reason()
				== RecommendationReliabilityReason.MISSING_ROLLBACK_BINDING) {
			return result(
					recommendationReliability,
					lifecycleUncertainty(apiResponse),
					RecommendationReliabilityIntegrationStatus.BLOCKED,
					RecommendationReliabilityIntegrationReason.MISSING_ROLLBACK_BINDING,
					RecommendationReliabilityIntegrationScope.ROLLBACK_UNCERTAINTY,
					false,
					false
			);
		}
		if (recommendationReliability.reason()
				== RecommendationReliabilityReason.MISSING_VERIFICATION_BINDING) {
			return result(
					recommendationReliability,
					lifecycleUncertainty(apiResponse),
					RecommendationReliabilityIntegrationStatus.BLOCKED,
					RecommendationReliabilityIntegrationReason.MISSING_VERIFICATION_BINDING,
					RecommendationReliabilityIntegrationScope.VERIFICATION_UNCERTAINTY,
					false,
					false
			);
		}
		return result(
				recommendationReliability,
				blocked(apiResponse),
				RecommendationReliabilityIntegrationStatus.BLOCKED,
				RecommendationReliabilityIntegrationReason
						.BLOCKED_RECOMMENDATION_RELIABILITY,
				RecommendationReliabilityIntegrationScope.RECOMMENDATION_FORBIDDEN,
				false,
				false
		);
	}

	private RecommendationReliabilityIntegrationResult result(
			RecommendationReliability recommendationReliability,
			EvidenceRuntimeApiResponse apiResponse,
			RecommendationReliabilityIntegrationStatus status,
			RecommendationReliabilityIntegrationReason reason,
			RecommendationReliabilityIntegrationScope scope,
			boolean operatorFacingRecommendationAllowed,
			boolean recommendationCertaintyAllowed
	) {
		return new RecommendationReliabilityIntegrationResult(
				recommendationReliability,
				apiResponse,
				status,
				reason,
				scope,
				operatorFacingRecommendationAllowed,
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
