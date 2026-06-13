package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceRuntimeSummaryResource {

	public EvidenceRuntimeSummaryResponse view(
			EvidenceRuntimeSummary summary
	) {
		Objects.requireNonNull(summary, "summary must not be null");

		return new EvidenceRuntimeSummaryResponse(
				summary.view(),
				resourceStatus(summary.summaryStatus()),
				resourceReason(summary.uncertaintyReason())
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private EvidenceRuntimeSummaryResourceStatus resourceStatus(
			EvidenceRuntimeSummaryStatus status
	) {
		return switch (status) {
			case HEALTHY -> EvidenceRuntimeSummaryResourceStatus.HEALTHY;
			case PARTIAL -> EvidenceRuntimeSummaryResourceStatus.PARTIAL;
			case UNCERTAIN -> EvidenceRuntimeSummaryResourceStatus.UNCERTAIN;
			case DEGRADED -> EvidenceRuntimeSummaryResourceStatus.DEGRADED;
			case FAILED -> EvidenceRuntimeSummaryResourceStatus.FAILED;
			case UNKNOWN -> EvidenceRuntimeSummaryResourceStatus.UNKNOWN;
		};
	}

	private EvidenceRuntimeSummaryResourceReason resourceReason(
			EvidenceRuntimeSummaryReason reason
	) {
		return switch (reason) {
			case PAYMENT_SAFETY_UNCERTAINTY ->
					EvidenceRuntimeSummaryResourceReason.PAYMENT_SAFETY_UNCERTAINTY;
			case PAYMENT_INCONSISTENCY ->
					EvidenceRuntimeSummaryResourceReason.PAYMENT_INCONSISTENCY;
			case ADAPTER_FAILURE ->
					EvidenceRuntimeSummaryResourceReason.ADAPTER_FAILURE;
			case PARTIAL_EVIDENCE ->
					EvidenceRuntimeSummaryResourceReason.PARTIAL_EVIDENCE;
			case UNKNOWN_EVIDENCE ->
					EvidenceRuntimeSummaryResourceReason.UNKNOWN_EVIDENCE;
			case CONTRADICTORY_EVIDENCE ->
					EvidenceRuntimeSummaryResourceReason.CONTRADICTORY_EVIDENCE;
			case OBSERVABILITY_UNAVAILABLE ->
					EvidenceRuntimeSummaryResourceReason.OBSERVABILITY_UNAVAILABLE;
			case UNKNOWN -> EvidenceRuntimeSummaryResourceReason.UNKNOWN;
		};
	}
}
