package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ActionAdmissionReliabilityIntegration {

	public ActionAdmissionReliabilityIntegrationResult integrate(
			ActionAdmissionReliability actionAdmissionReliability
	) {
		Objects.requireNonNull(
				actionAdmissionReliability,
				"actionAdmissionReliability must not be null"
		);

		EvidenceRuntimeApiResponse apiResponse = actionAdmissionReliability
				.verificationReliability()
				.approvalReliability()
				.recommendationReliability()
				.decisionReliability()
				.assessmentReliability()
				.evidenceReliability()
				.trustScore()
				.lineageIntegrationResult()
				.apiResponse();

		if (actionAdmissionReliability.level() == ActionAdmissionReliabilityLevel.BLOCKED) {
			return blockedResult(actionAdmissionReliability, apiResponse);
		}
		if (actionAdmissionReliability.level() == ActionAdmissionReliabilityLevel.UNRELIABLE) {
			return result(
					actionAdmissionReliability,
					unreliable(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.UNRELIABLE,
					ActionAdmissionReliabilityIntegrationReason
							.UNRELIABLE_ACTION_ADMISSION_RELIABILITY,
					ActionAdmissionReliabilityIntegrationScope.ACTION_ADMISSION_UNCERTAINTY,
					false,
					false
			);
		}
		if (actionAdmissionReliability.reason()
				== ActionAdmissionReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY) {
			return result(
					actionAdmissionReliability,
					paymentCriticalRisk(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.WARNING,
					ActionAdmissionReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ActionAdmissionReliabilityIntegrationScope.PAYMENT_CRITICAL_RISK_VIEW,
					false,
					false
			);
		}
		if (actionAdmissionReliability.reason()
				== ActionAdmissionReliabilityReason.CONTRADICTORY_VERIFICATION) {
			return result(
					actionAdmissionReliability,
					lifecycleUncertainty(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.WARNING,
					ActionAdmissionReliabilityIntegrationReason.CONTRADICTORY_ACTION_ADMISSION,
					ActionAdmissionReliabilityIntegrationScope.LIFECYCLE_UNCERTAINTY,
					false,
					false
			);
		}
		if (actionAdmissionReliability.level() == ActionAdmissionReliabilityLevel.LOW) {
			return result(
					actionAdmissionReliability,
					warning(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.WARNING,
					ActionAdmissionReliabilityIntegrationReason.LOW_ACTION_ADMISSION_RELIABILITY,
					ActionAdmissionReliabilityIntegrationScope.OPERATOR_WARNING_VIEW,
					false,
					false
			);
		}
		if (actionAdmissionReliability.level() == ActionAdmissionReliabilityLevel.MEDIUM) {
			return result(
					actionAdmissionReliability,
					partial(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.PARTIAL_ADMISSION_READINESS,
					ActionAdmissionReliabilityIntegrationReason
							.MEDIUM_ACTION_ADMISSION_RELIABILITY,
					ActionAdmissionReliabilityIntegrationScope.PARTIAL_ADMISSION_VIEW,
					false,
					false
			);
		}
		if (actionAdmissionReliability.level() == ActionAdmissionReliabilityLevel.HIGH) {
			return result(
					actionAdmissionReliability,
					apiResponse,
					ActionAdmissionReliabilityIntegrationStatus.ACTION_ADMISSION_READY,
					ActionAdmissionReliabilityIntegrationReason.HIGH_ACTION_ADMISSION_RELIABILITY,
					ActionAdmissionReliabilityIntegrationScope.ACTION_ADMISSION_READY_VIEW,
					true,
					true
			);
		}

		return result(
				actionAdmissionReliability,
				unknown(apiResponse),
				ActionAdmissionReliabilityIntegrationStatus.UNKNOWN,
				ActionAdmissionReliabilityIntegrationReason.UNKNOWN,
				ActionAdmissionReliabilityIntegrationScope.UNKNOWN,
				false,
				false
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesActionAdmission() {
		return false;
	}

	public boolean actionAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private ActionAdmissionReliabilityIntegrationResult blockedResult(
			ActionAdmissionReliability actionAdmissionReliability,
			EvidenceRuntimeApiResponse apiResponse
	) {
		if (actionAdmissionReliability.reason()
				== ActionAdmissionReliabilityReason.MISSING_ACTION_TYPE) {
			return result(
					actionAdmissionReliability,
					lifecycleUncertainty(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.BLOCKED,
					ActionAdmissionReliabilityIntegrationReason.MISSING_ACTION_TYPE,
					ActionAdmissionReliabilityIntegrationScope.ACTION_TYPE_UNCERTAINTY,
					false,
					false
			);
		}
		if (actionAdmissionReliability.reason()
				== ActionAdmissionReliabilityReason.MISSING_BLAST_RADIUS_BOUNDARY) {
			return result(
					actionAdmissionReliability,
					lifecycleUncertainty(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.BLOCKED,
					ActionAdmissionReliabilityIntegrationReason.MISSING_BLAST_RADIUS_BOUNDARY,
					ActionAdmissionReliabilityIntegrationScope.BLAST_RADIUS_UNCERTAINTY,
					false,
					false
			);
		}
		if (actionAdmissionReliability.reason()
				== ActionAdmissionReliabilityReason.MISSING_ROLLBACK_BINDING) {
			return result(
					actionAdmissionReliability,
					lifecycleUncertainty(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.BLOCKED,
					ActionAdmissionReliabilityIntegrationReason.MISSING_ROLLBACK_BINDING,
					ActionAdmissionReliabilityIntegrationScope.ROLLBACK_UNCERTAINTY,
					false,
					false
			);
		}
		if (actionAdmissionReliability.reason()
				== ActionAdmissionReliabilityReason.MISSING_VERIFICATION_BINDING) {
			return result(
					actionAdmissionReliability,
					lifecycleUncertainty(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.BLOCKED,
					ActionAdmissionReliabilityIntegrationReason.MISSING_VERIFICATION_BINDING,
					ActionAdmissionReliabilityIntegrationScope.VERIFICATION_BINDING_UNCERTAINTY,
					false,
					false
			);
		}
		if (actionAdmissionReliability.reason()
				== ActionAdmissionReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT) {
			return result(
					actionAdmissionReliability,
					lifecycleUncertainty(apiResponse),
					ActionAdmissionReliabilityIntegrationStatus.BLOCKED,
					ActionAdmissionReliabilityIntegrationReason
							.MISSING_HUMAN_APPROVAL_REQUIREMENT,
					ActionAdmissionReliabilityIntegrationScope.HUMAN_APPROVAL_UNCERTAINTY,
					false,
					false
			);
		}
		return result(
				actionAdmissionReliability,
				blocked(apiResponse),
				ActionAdmissionReliabilityIntegrationStatus.BLOCKED,
				ActionAdmissionReliabilityIntegrationReason.BLOCKED_ACTION_ADMISSION_RELIABILITY,
				ActionAdmissionReliabilityIntegrationScope.ACTION_COMMAND_CANDIDATE_FORBIDDEN,
				false,
				false
		);
	}

	private ActionAdmissionReliabilityIntegrationResult result(
			ActionAdmissionReliability actionAdmissionReliability,
			EvidenceRuntimeApiResponse apiResponse,
			ActionAdmissionReliabilityIntegrationStatus status,
			ActionAdmissionReliabilityIntegrationReason reason,
			ActionAdmissionReliabilityIntegrationScope scope,
			boolean actionCommandCandidateVisible,
			boolean actionAdmissionCertaintyAllowed
	) {
		return new ActionAdmissionReliabilityIntegrationResult(
				actionAdmissionReliability,
				apiResponse,
				status,
				reason,
				scope,
				actionCommandCandidateVisible,
				actionAdmissionCertaintyAllowed
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
						EvidenceRuntimeSummaryStatus.PARTIAL,
						OperationalUncertainty.MODERATE,
						summary.paymentSafetyState(),
						true,
						summary.uncertaintyReason(),
						summary.auditTrusted(),
						EvidenceCompleteness.PARTIAL
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
						EvidenceRuntimeSummaryReason.PAYMENT_SAFETY_UNCERTAINTY,
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
						EvidenceRuntimeSummaryReason.UNKNOWN_EVIDENCE,
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
						OperationalUncertainty.HIGH,
						summary.paymentSafetyState(),
						true,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						false,
						summary.evidenceCompleteness()
				),
				EvidenceRuntimeApiStatus.UNKNOWN,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}
}
