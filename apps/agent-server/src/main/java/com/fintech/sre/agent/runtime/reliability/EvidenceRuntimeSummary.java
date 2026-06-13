package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceRuntimeSummary(
		EvidenceRuntimeSummaryStatus summaryStatus,
		OperationalUncertainty riskLevel,
		OperationalUncertainty paymentSafetyState,
		boolean uncertaintyDetected,
		EvidenceRuntimeSummaryReason uncertaintyReason,
		boolean auditTrusted,
		EvidenceCompleteness evidenceCompleteness
) {
	public EvidenceRuntimeSummary {
		Objects.requireNonNull(summaryStatus, "summaryStatus must not be null");
		Objects.requireNonNull(riskLevel, "riskLevel must not be null");
		Objects.requireNonNull(
				paymentSafetyState,
				"paymentSafetyState must not be null"
		);
		Objects.requireNonNull(
				uncertaintyReason,
				"uncertaintyReason must not be null"
		);
		Objects.requireNonNull(
				evidenceCompleteness,
				"evidenceCompleteness must not be null"
		);
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

	public boolean actionAdmissionResult() {
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

	public EvidenceRuntimeSummaryView view() {
		return new EvidenceRuntimeSummaryView(
				summaryStatus,
				riskLevel,
				paymentSafetyState,
				uncertaintyDetected,
				uncertaintyReason,
				auditTrusted,
				evidenceCompleteness
		);
	}
}
