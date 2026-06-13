package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceLineageIntegration {

	public EvidenceLineageIntegrationResult integrate(
			EvidenceRuntimeApiBoundary apiBoundary,
			EvidenceRuntimeApiRequest apiRequest,
			EvidenceLineage lineage
	) {
		Objects.requireNonNull(apiBoundary, "apiBoundary must not be null");
		Objects.requireNonNull(apiRequest, "apiRequest must not be null");
		Objects.requireNonNull(lineage, "lineage must not be null");

		EvidenceRuntimeApiResponse apiResponse = apiBoundary.read(apiRequest);

		if (lineage.status() == EvidenceLineageStatus.BLOCKED) {
			return result(
					lineage,
					apiResponse,
					EvidenceLineageIntegrationStatus.BLOCKED,
					EvidenceLineageIntegrationReason.BLOCKED_LINEAGE,
					EvidenceLineageIntegrationScope.API_BLOCKED
			);
		}
		if (lineage.status() == EvidenceLineageStatus.INCOMPLETE) {
			return result(
					lineage,
					downgradeAuditTrust(apiResponse),
					EvidenceLineageIntegrationStatus.UNTRUSTED,
					lineage.reason() == EvidenceLineageReason.MISSING_PROVENANCE
							? EvidenceLineageIntegrationReason.MISSING_PROVENANCE_LINEAGE
							: EvidenceLineageIntegrationReason.INCOMPLETE_LINEAGE,
					EvidenceLineageIntegrationScope.TRUST_DOWNGRADED
			);
		}
		if (lineage.status() == EvidenceLineageStatus.RESTRICTED) {
			return result(
					lineage,
					restricted(apiResponse),
					EvidenceLineageIntegrationStatus.RESTRICTED,
					lineage.reason() == EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE
							? EvidenceLineageIntegrationReason.PAYMENT_LINEAGE_RESTRICTED
							: EvidenceLineageIntegrationReason.RESTRICTED_LINEAGE,
					EvidenceLineageIntegrationScope.OPERATOR_FACING_RESTRICTED
			);
		}
		if (lineage.reason() == EvidenceLineageReason.CONTRADICTORY_EVIDENCE) {
			return result(
					lineage,
					elevateUncertainty(apiResponse),
					EvidenceLineageIntegrationStatus.RESTRICTED,
					EvidenceLineageIntegrationReason.CONTRADICTORY_LINEAGE_RISK,
					EvidenceLineageIntegrationScope.OPERATOR_FACING_RESTRICTED
			);
		}
		if (lineage.status() == EvidenceLineageStatus.UNKNOWN) {
			return result(
					lineage,
					unknown(apiResponse),
					EvidenceLineageIntegrationStatus.UNKNOWN,
					EvidenceLineageIntegrationReason.UNKNOWN,
					EvidenceLineageIntegrationScope.UNKNOWN
			);
		}

		return result(
				lineage,
				apiResponse,
				EvidenceLineageIntegrationStatus.INTEGRATED,
				EvidenceLineageIntegrationReason.UNKNOWN,
				EvidenceLineageIntegrationScope.API_EXPOSURE
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

	private EvidenceLineageIntegrationResult result(
			EvidenceLineage lineage,
			EvidenceRuntimeApiResponse apiResponse,
			EvidenceLineageIntegrationStatus status,
			EvidenceLineageIntegrationReason reason,
			EvidenceLineageIntegrationScope scope
	) {
		return new EvidenceLineageIntegrationResult(
				lineage,
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

	private EvidenceRuntimeApiResponse restricted(
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
