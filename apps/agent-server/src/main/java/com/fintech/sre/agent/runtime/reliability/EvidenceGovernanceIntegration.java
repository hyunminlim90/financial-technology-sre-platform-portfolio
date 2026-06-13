package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceGovernanceIntegration {

	public EvidenceGovernanceIntegrationResult integrate(
			EvidenceRuntimeApiBoundary apiBoundary,
			EvidenceRuntimeApiRequest apiRequest,
			EvidenceGovernancePolicy governancePolicy
	) {
		Objects.requireNonNull(apiBoundary, "apiBoundary must not be null");
		Objects.requireNonNull(apiRequest, "apiRequest must not be null");
		Objects.requireNonNull(
				governancePolicy,
				"governancePolicy must not be null"
		);

		EvidenceRuntimeApiResponse apiResponse = apiBoundary.read(apiRequest);

		if (governancePolicy.classification() == EvidenceClassification.BLOCKED) {
			return result(
					governancePolicy,
					apiResponse,
					EvidenceGovernanceIntegrationStatus.BLOCKED,
					EvidenceGovernanceIntegrationReason.BLOCKED_CLASSIFICATION,
					EvidenceGovernanceIntegrationScope.API_BLOCKED
			);
		}
		if (governancePolicy.provenance().provenanceMissing()) {
			return result(
					governancePolicy,
					downgradeAuditTrust(apiResponse),
					EvidenceGovernanceIntegrationStatus.UNTRUSTED,
					EvidenceGovernanceIntegrationReason.MISSING_PROVENANCE,
					EvidenceGovernanceIntegrationScope.TRUST_DOWNGRADED
			);
		}
		if (governancePolicy.trustLevel() == EvidenceTrustLevel.UNTRUSTED
				|| governancePolicy.trustLevel() == EvidenceTrustLevel.UNKNOWN) {
			return result(
					governancePolicy,
					downgradeAuditTrust(apiResponse),
					EvidenceGovernanceIntegrationStatus.UNTRUSTED,
					EvidenceGovernanceIntegrationReason.UNTRUSTED_EVIDENCE,
					EvidenceGovernanceIntegrationScope.TRUST_DOWNGRADED
			);
		}
		if (!governancePolicy.operatorFacingAllowed()) {
			return result(
					governancePolicy,
					apiResponse,
					EvidenceGovernanceIntegrationStatus.RESTRICTED,
					governancePolicy.classification()
							== EvidenceClassification.GOVERNANCE_PROTECTED
									? EvidenceGovernanceIntegrationReason
											.GOVERNANCE_PROTECTED_CLASSIFICATION
									: EvidenceGovernanceIntegrationReason.UNSANITIZED_EVIDENCE,
					EvidenceGovernanceIntegrationScope.OPERATOR_FACING_RESTRICTED
			);
		}
		if (governancePolicy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY) {
			return result(
					governancePolicy,
					elevateUncertainty(apiResponse),
					EvidenceGovernanceIntegrationStatus.RESTRICTED,
					EvidenceGovernanceIntegrationReason.CONTRADICTORY_INTEGRITY,
					EvidenceGovernanceIntegrationScope.OPERATOR_FACING_RESTRICTED
			);
		}
		if (governancePolicy.classification() == EvidenceClassification.RESTRICTED) {
			return result(
					governancePolicy,
					apiResponse,
					EvidenceGovernanceIntegrationStatus.RESTRICTED,
					EvidenceGovernanceIntegrationReason.PAYMENT_RESTRICTED_CLASSIFICATION,
					EvidenceGovernanceIntegrationScope.OPERATOR_FACING_RESTRICTED
			);
		}

		return result(
				governancePolicy,
				apiResponse,
				EvidenceGovernanceIntegrationStatus.INTEGRATED,
				EvidenceGovernanceIntegrationReason.UNKNOWN,
				EvidenceGovernanceIntegrationScope.API_EXPOSURE
		);
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

	private EvidenceGovernanceIntegrationResult result(
			EvidenceGovernancePolicy governancePolicy,
			EvidenceRuntimeApiResponse apiResponse,
			EvidenceGovernanceIntegrationStatus status,
			EvidenceGovernanceIntegrationReason reason,
			EvidenceGovernanceIntegrationScope scope
	) {
		return new EvidenceGovernanceIntegrationResult(
				governancePolicy,
				apiResponse,
				status,
				reason,
				scope
		);
	}

	private EvidenceRuntimeApiResponse downgradeAuditTrust(
			EvidenceRuntimeApiResponse apiResponse
	) {
		EvidenceRuntimeSummaryView summary = apiResponse.summary();
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						summary.summaryStatus(),
						summary.riskLevel(),
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

	private EvidenceRuntimeApiResponse elevateUncertainty(
			EvidenceRuntimeApiResponse apiResponse
	) {
		EvidenceRuntimeSummaryView summary = apiResponse.summary();
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.UNCERTAIN,
						OperationalUncertainty.HIGH,
						summary.paymentSafetyState(),
						true,
						EvidenceRuntimeSummaryReason.CONTRADICTORY_EVIDENCE,
						summary.auditTrusted(),
						summary.evidenceCompleteness()
				),
				EvidenceRuntimeApiStatus.UNCERTAIN,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}
}
