package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class DecisionReliabilityIntegration {

	public DecisionReliabilityIntegrationResult integrate(
			DecisionReliability decisionReliability
	) {
		Objects.requireNonNull(
				decisionReliability,
				"decisionReliability must not be null"
		);

		EvidenceRuntimeApiResponse apiResponse = decisionReliability
				.assessmentReliability()
				.evidenceReliability()
				.trustScore()
				.lineageIntegrationResult()
				.apiResponse();

		if (decisionReliability.level() == DecisionReliabilityLevel.BLOCKED) {
			return blockedResult(decisionReliability, apiResponse);
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.UNRELIABLE) {
			return result(
					decisionReliability,
					unreliable(apiResponse),
					DecisionReliabilityIntegrationStatus.UNRELIABLE,
					DecisionReliabilityIntegrationReason.UNRELIABLE_DECISION_RELIABILITY,
					DecisionReliabilityIntegrationScope.RECOMMENDATION_UNCERTAINTY,
					false,
					false
			);
		}
		if (decisionReliability.reason()
				== DecisionReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY) {
			return result(
					decisionReliability,
					paymentRisk(apiResponse),
					DecisionReliabilityIntegrationStatus.WARNING,
					DecisionReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					DecisionReliabilityIntegrationScope.PAYMENT_RISK_VIEW,
					false,
					false
			);
		}
		if (decisionReliability.reason()
				== DecisionReliabilityReason.CONTRADICTORY_ASSESSMENT) {
			return result(
					decisionReliability,
					lifecycleUncertainty(apiResponse),
					DecisionReliabilityIntegrationStatus.WARNING,
					DecisionReliabilityIntegrationReason.CONTRADICTORY_DECISION,
					DecisionReliabilityIntegrationScope.LIFECYCLE_UNCERTAINTY,
					false,
					false
			);
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.LOW) {
			return result(
					decisionReliability,
					warning(apiResponse),
					DecisionReliabilityIntegrationStatus.WARNING,
					DecisionReliabilityIntegrationReason.LOW_DECISION_RELIABILITY,
					DecisionReliabilityIntegrationScope.OPERATOR_WARNING_VIEW,
					false,
					false
			);
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.MEDIUM) {
			return result(
					decisionReliability,
					partial(apiResponse),
					DecisionReliabilityIntegrationStatus.PARTIAL_DECISION_RELIABILITY,
					DecisionReliabilityIntegrationReason.MEDIUM_DECISION_RELIABILITY,
					DecisionReliabilityIntegrationScope.PARTIAL_DECISION_VIEW,
					false,
					false
			);
		}
		if (decisionReliability.level() == DecisionReliabilityLevel.HIGH) {
			return result(
					decisionReliability,
					apiResponse,
					DecisionReliabilityIntegrationStatus.RELIABLE,
					DecisionReliabilityIntegrationReason.HIGH_DECISION_RELIABILITY,
					DecisionReliabilityIntegrationScope.RELIABLE_DECISION_VIEW,
					true,
					true
			);
		}

		return result(
				decisionReliability,
				unknown(apiResponse),
				DecisionReliabilityIntegrationStatus.UNKNOWN,
				DecisionReliabilityIntegrationReason.UNKNOWN,
				DecisionReliabilityIntegrationScope.UNKNOWN,
				false,
				false
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesDecision() {
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

	private DecisionReliabilityIntegrationResult blockedResult(
			DecisionReliability decisionReliability,
			EvidenceRuntimeApiResponse apiResponse
	) {
		if (decisionReliability.reason() == DecisionReliabilityReason.MISSING_SCENARIO_BINDING) {
			return result(
					decisionReliability,
					lifecycleUncertainty(apiResponse),
					DecisionReliabilityIntegrationStatus.BLOCKED,
					DecisionReliabilityIntegrationReason.MISSING_SCENARIO_BINDING,
					DecisionReliabilityIntegrationScope.SCENARIO_UNCERTAINTY,
					false,
					false
			);
		}
		if (decisionReliability.reason() == DecisionReliabilityReason.MISSING_ROLLBACK_BINDING) {
			return result(
					decisionReliability,
					lifecycleUncertainty(apiResponse),
					DecisionReliabilityIntegrationStatus.BLOCKED,
					DecisionReliabilityIntegrationReason.MISSING_ROLLBACK_BINDING,
					DecisionReliabilityIntegrationScope.ROLLBACK_UNCERTAINTY,
					false,
					false
			);
		}
		if (decisionReliability.reason() == DecisionReliabilityReason.MISSING_VERIFICATION_BINDING) {
			return result(
					decisionReliability,
					lifecycleUncertainty(apiResponse),
					DecisionReliabilityIntegrationStatus.BLOCKED,
					DecisionReliabilityIntegrationReason.MISSING_VERIFICATION_BINDING,
					DecisionReliabilityIntegrationScope.VERIFICATION_UNCERTAINTY,
					false,
					false
			);
		}
		return result(
				decisionReliability,
				blocked(apiResponse),
				DecisionReliabilityIntegrationStatus.BLOCKED,
				DecisionReliabilityIntegrationReason.BLOCKED_DECISION_RELIABILITY,
				DecisionReliabilityIntegrationScope.LIFECYCLE_BLOCKED,
				false,
				false
		);
	}

	private DecisionReliabilityIntegrationResult result(
			DecisionReliability decisionReliability,
			EvidenceRuntimeApiResponse apiResponse,
			DecisionReliabilityIntegrationStatus status,
			DecisionReliabilityIntegrationReason reason,
			DecisionReliabilityIntegrationScope scope,
			boolean lifecycleStableAllowed,
			boolean recommendationCertaintyAllowed
	) {
		return new DecisionReliabilityIntegrationResult(
				decisionReliability,
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
