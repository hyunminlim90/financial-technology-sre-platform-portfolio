package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceRuntimeApiResponse(
		EvidenceRuntimeSummaryView summary,
		EvidenceRuntimeApiStatus status,
		EvidenceRuntimeApiRejectionReason rejectionReason
) {
	public EvidenceRuntimeApiResponse {
		Objects.requireNonNull(summary, "summary must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(rejectionReason, "rejectionReason must not be null");
	}

	public boolean readOnly() {
		return true;
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

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
