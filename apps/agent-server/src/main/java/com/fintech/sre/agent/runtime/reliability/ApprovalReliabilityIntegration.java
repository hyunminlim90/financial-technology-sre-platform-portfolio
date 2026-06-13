package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ApprovalReliabilityIntegration {

	public ApprovalReliabilityIntegrationResult integrate(
			ApprovalReliability approvalReliability
	) {
		Objects.requireNonNull(
				approvalReliability,
				"approvalReliability must not be null"
		);

		EvidenceRuntimeApiResponse apiResponse = approvalReliability
				.recommendationReliability()
				.decisionReliability()
				.assessmentReliability()
				.evidenceReliability()
				.trustScore()
				.lineageIntegrationResult()
				.apiResponse();

		if (approvalReliability.level() == ApprovalReliabilityLevel.BLOCKED) {
			return blockedResult(approvalReliability, apiResponse);
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.UNRELIABLE) {
			return result(
					approvalReliability,
					unreliable(apiResponse),
					ApprovalReliabilityIntegrationStatus.UNRELIABLE,
					ApprovalReliabilityIntegrationReason.UNRELIABLE_APPROVAL_RELIABILITY,
					ApprovalReliabilityIntegrationScope.APPROVAL_UNCERTAINTY,
					false,
					false
			);
		}
		if (approvalReliability.reason() == ApprovalReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY) {
			return result(
					approvalReliability,
					paymentCriticalRisk(apiResponse),
					ApprovalReliabilityIntegrationStatus.WARNING,
					ApprovalReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ApprovalReliabilityIntegrationScope.PAYMENT_CRITICAL_RISK_VIEW,
					false,
					false
			);
		}
		if (approvalReliability.reason() == ApprovalReliabilityReason.CONTRADICTORY_RECOMMENDATION) {
			return result(
					approvalReliability,
					lifecycleUncertainty(apiResponse),
					ApprovalReliabilityIntegrationStatus.WARNING,
					ApprovalReliabilityIntegrationReason.CONTRADICTORY_APPROVAL,
					ApprovalReliabilityIntegrationScope.LIFECYCLE_UNCERTAINTY,
					false,
					false
			);
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.LOW) {
			return result(
					approvalReliability,
					warning(apiResponse),
					ApprovalReliabilityIntegrationStatus.WARNING,
					ApprovalReliabilityIntegrationReason.LOW_APPROVAL_RELIABILITY,
					ApprovalReliabilityIntegrationScope.OPERATOR_WARNING_VIEW,
					false,
					false
			);
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.MEDIUM) {
			return result(
					approvalReliability,
					partial(apiResponse),
					ApprovalReliabilityIntegrationStatus.PARTIAL_APPROVAL_READINESS,
					ApprovalReliabilityIntegrationReason.MEDIUM_APPROVAL_RELIABILITY,
					ApprovalReliabilityIntegrationScope.PARTIAL_APPROVAL_VIEW,
					false,
					false
			);
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.HIGH) {
			return result(
					approvalReliability,
					apiResponse,
					ApprovalReliabilityIntegrationStatus.APPROVAL_READY,
					ApprovalReliabilityIntegrationReason.HIGH_APPROVAL_RELIABILITY,
					ApprovalReliabilityIntegrationScope.APPROVAL_READY_VIEW,
					true,
					true
			);
		}

		return result(
				approvalReliability,
				unknown(apiResponse),
				ApprovalReliabilityIntegrationStatus.UNKNOWN,
				ApprovalReliabilityIntegrationReason.UNKNOWN,
				ApprovalReliabilityIntegrationScope.UNKNOWN,
				false,
				false
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesApproval() {
		return false;
	}

	public boolean approvalAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private ApprovalReliabilityIntegrationResult blockedResult(
			ApprovalReliability approvalReliability,
			EvidenceRuntimeApiResponse apiResponse
	) {
		if (approvalReliability.reason() == ApprovalReliabilityReason.MISSING_OPERATOR_CONTEXT) {
			return result(
					approvalReliability,
					lifecycleUncertainty(apiResponse),
					ApprovalReliabilityIntegrationStatus.BLOCKED,
					ApprovalReliabilityIntegrationReason.MISSING_OPERATOR_CONTEXT,
					ApprovalReliabilityIntegrationScope.OPERATOR_CONTEXT_UNCERTAINTY,
					false,
					false
			);
		}
		if (approvalReliability.reason()
				== ApprovalReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT) {
			return result(
					approvalReliability,
					lifecycleUncertainty(apiResponse),
					ApprovalReliabilityIntegrationStatus.BLOCKED,
					ApprovalReliabilityIntegrationReason.MISSING_HUMAN_APPROVAL_REQUIREMENT,
					ApprovalReliabilityIntegrationScope.HUMAN_APPROVAL_UNCERTAINTY,
					false,
					false
			);
		}
		if (approvalReliability.reason() == ApprovalReliabilityReason.MISSING_ROLLBACK_BINDING) {
			return result(
					approvalReliability,
					lifecycleUncertainty(apiResponse),
					ApprovalReliabilityIntegrationStatus.BLOCKED,
					ApprovalReliabilityIntegrationReason.MISSING_ROLLBACK_BINDING,
					ApprovalReliabilityIntegrationScope.ROLLBACK_UNCERTAINTY,
					false,
					false
			);
		}
		if (approvalReliability.reason()
				== ApprovalReliabilityReason.MISSING_VERIFICATION_BINDING) {
			return result(
					approvalReliability,
					lifecycleUncertainty(apiResponse),
					ApprovalReliabilityIntegrationStatus.BLOCKED,
					ApprovalReliabilityIntegrationReason.MISSING_VERIFICATION_BINDING,
					ApprovalReliabilityIntegrationScope.VERIFICATION_UNCERTAINTY,
					false,
					false
			);
		}
		return result(
				approvalReliability,
				blocked(apiResponse),
				ApprovalReliabilityIntegrationStatus.BLOCKED,
				ApprovalReliabilityIntegrationReason.BLOCKED_APPROVAL_RELIABILITY,
				ApprovalReliabilityIntegrationScope.APPROVAL_FORBIDDEN,
				false,
				false
		);
	}

	private ApprovalReliabilityIntegrationResult result(
			ApprovalReliability approvalReliability,
			EvidenceRuntimeApiResponse apiResponse,
			ApprovalReliabilityIntegrationStatus status,
			ApprovalReliabilityIntegrationReason reason,
			ApprovalReliabilityIntegrationScope scope,
			boolean approvalRequestAllowed,
			boolean approvalCertaintyAllowed
	) {
		return new ApprovalReliabilityIntegrationResult(
				approvalReliability,
				apiResponse,
				status,
				reason,
				scope,
				approvalRequestAllowed,
				approvalCertaintyAllowed
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
