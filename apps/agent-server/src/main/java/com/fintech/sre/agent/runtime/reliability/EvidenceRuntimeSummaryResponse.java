package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceRuntimeSummaryResponse(
		EvidenceRuntimeSummaryView summary,
		EvidenceRuntimeSummaryResourceStatus resourceStatus,
		EvidenceRuntimeSummaryResourceReason resourceReason
) {
	public EvidenceRuntimeSummaryResponse {
		Objects.requireNonNull(summary, "summary must not be null");
		Objects.requireNonNull(
				resourceStatus,
				"resourceStatus must not be null"
		);
		Objects.requireNonNull(
				resourceReason,
				"resourceReason must not be null"
		);
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmission() {
		return false;
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean exposesVendorDetail() {
		return false;
	}

	public boolean exposesCredentialConfiguration() {
		return false;
	}
}
