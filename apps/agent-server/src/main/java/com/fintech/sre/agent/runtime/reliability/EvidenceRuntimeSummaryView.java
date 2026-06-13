package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceRuntimeSummaryView(
		EvidenceRuntimeSummaryStatus summaryStatus,
		OperationalUncertainty riskLevel,
		OperationalUncertainty paymentSafetyState,
		boolean uncertaintyDetected,
		EvidenceRuntimeSummaryReason uncertaintyReason,
		boolean auditTrusted,
		EvidenceCompleteness evidenceCompleteness
) {
	public EvidenceRuntimeSummaryView {
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

	public boolean operatorFacingOnly() {
		return true;
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
