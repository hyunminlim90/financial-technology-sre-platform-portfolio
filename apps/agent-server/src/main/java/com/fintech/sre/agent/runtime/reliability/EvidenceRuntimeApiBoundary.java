package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceRuntimeApiBoundary {

	public EvidenceRuntimeApiResponse read(
			EvidenceRuntimeApiRequest request
	) {
		Objects.requireNonNull(request, "request must not be null");

		if (request.summaryResource() == null) {
			return rejected(
					fallbackSummary(request.summary()),
					EvidenceRuntimeApiRejectionReason.MISSING_SUMMARY_RESOURCE
			);
		}

		EvidenceRuntimeSummaryResponse response = request.summaryResource()
				.view(Objects.requireNonNull(request.summary(), "summary must not be null"));

		if (response.exposesRawPayload()) {
			return rejected(
					response.summary(),
					EvidenceRuntimeApiRejectionReason.RAW_PAYLOAD_EXPOSURE_BLOCKED
			);
		}
		if (response.exposesVendorDetail()) {
			return rejected(
					response.summary(),
					EvidenceRuntimeApiRejectionReason.VENDOR_DETAIL_EXPOSURE_BLOCKED
			);
		}
		if (response.exposesCredentialConfiguration()) {
			return rejected(
					response.summary(),
					EvidenceRuntimeApiRejectionReason.CREDENTIAL_EXPOSURE_BLOCKED
			);
		}
		if (!response.summary().auditTrusted()) {
			return new EvidenceRuntimeApiResponse(
					response.summary(),
					EvidenceRuntimeApiStatus.UNTRUSTED,
					EvidenceRuntimeApiRejectionReason.UNTRUSTED_AUDIT
			);
		}

		return new EvidenceRuntimeApiResponse(
				response.summary(),
				status(response),
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}

	public boolean actualHttpEndpoint() {
		return false;
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private EvidenceRuntimeApiResponse rejected(
			EvidenceRuntimeSummaryView summary,
			EvidenceRuntimeApiRejectionReason reason
	) {
		return new EvidenceRuntimeApiResponse(
				summary,
				EvidenceRuntimeApiStatus.REJECTED,
				reason
		);
	}

	private EvidenceRuntimeApiStatus status(
			EvidenceRuntimeSummaryResponse response
	) {
		if (response.summary().summaryStatus() == EvidenceRuntimeSummaryStatus.UNKNOWN) {
			return EvidenceRuntimeApiStatus.UNKNOWN;
		}
		if (response.summary().uncertaintyDetected()) {
			return EvidenceRuntimeApiStatus.UNCERTAIN;
		}
		return EvidenceRuntimeApiStatus.READABLE;
	}

	private EvidenceRuntimeSummaryView fallbackSummary(
			EvidenceRuntimeSummary summary
	) {
		if (summary == null) {
			return new EvidenceRuntimeSummaryView(
					EvidenceRuntimeSummaryStatus.UNKNOWN,
					OperationalUncertainty.HIGH,
					OperationalUncertainty.HIGH,
					true,
					EvidenceRuntimeSummaryReason.UNKNOWN,
					false,
					EvidenceCompleteness.ABSENT
			);
		}
		return summary.view();
	}
}
