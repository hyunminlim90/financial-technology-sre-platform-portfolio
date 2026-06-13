package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceRuntimeSummaryBuilder {

	public EvidenceRuntimeSummary build(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		Objects.requireNonNull(
				pipelineResult,
				"pipelineResult must not be null"
		);

		return new EvidenceRuntimeSummary(
				status(pipelineResult),
				riskLevel(pipelineResult),
				paymentSafetyState(pipelineResult),
				uncertaintyDetected(pipelineResult),
				reason(pipelineResult),
				auditTrusted(pipelineResult),
				evidenceCompleteness(pipelineResult)
		);
	}

	public EvidenceRuntimeSummaryView view(
			EvidenceRuntimeSummary summary
	) {
		Objects.requireNonNull(summary, "summary must not be null");
		return summary.view();
	}

	private EvidenceRuntimeSummaryStatus status(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		if (paymentInconsistencyDetected(pipelineResult)) {
			return EvidenceRuntimeSummaryStatus.DEGRADED;
		}
		if (adapterFailureDetected(pipelineResult)
				|| paymentSafetyUncertain(pipelineResult)
				|| unknownEvidenceDetected(pipelineResult)
				|| contradictoryEvidenceDetected(pipelineResult)) {
			return EvidenceRuntimeSummaryStatus.UNCERTAIN;
		}
		return switch (evidenceCompleteness(pipelineResult)) {
			case COMPLETE -> EvidenceRuntimeSummaryStatus.HEALTHY;
			case PARTIAL -> EvidenceRuntimeSummaryStatus.PARTIAL;
			case ABSENT -> EvidenceRuntimeSummaryStatus.UNKNOWN;
		};
	}

	private OperationalUncertainty riskLevel(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		if (paymentInconsistencyDetected(pipelineResult)) {
			return OperationalUncertainty.CRITICAL;
		}
		return pipelineResult.observableRuntimeResult()
				.summaryResponse()
				.summary()
				.risk();
	}

	private OperationalUncertainty paymentSafetyState(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		if (paymentInconsistencyDetected(pipelineResult)) {
			return OperationalUncertainty.CRITICAL;
		}
		if (paymentSafetyUncertain(pipelineResult)) {
			return pipelineResult.observableRuntimeResult()
					.assessmentPipelineResult()
					.assessmentResult()
					.overallRisk();
		}
		return OperationalUncertainty.LOW;
	}

	private boolean uncertaintyDetected(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return status(pipelineResult) != EvidenceRuntimeSummaryStatus.HEALTHY;
	}

	private EvidenceRuntimeSummaryReason reason(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		if (paymentInconsistencyDetected(pipelineResult)) {
			return EvidenceRuntimeSummaryReason.PAYMENT_INCONSISTENCY;
		}
		if (adapterFailureDetected(pipelineResult)) {
			return EvidenceRuntimeSummaryReason.ADAPTER_FAILURE;
		}
		if (paymentSafetyUncertain(pipelineResult)) {
			return EvidenceRuntimeSummaryReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (contradictoryEvidenceDetected(pipelineResult)) {
			return EvidenceRuntimeSummaryReason.CONTRADICTORY_EVIDENCE;
		}
		if (partialEvidenceDetected(pipelineResult)) {
			return EvidenceRuntimeSummaryReason.PARTIAL_EVIDENCE;
		}
		if (observabilityUnavailable(pipelineResult)) {
			return EvidenceRuntimeSummaryReason.OBSERVABILITY_UNAVAILABLE;
		}
		if (unknownEvidenceDetected(pipelineResult)) {
			return EvidenceRuntimeSummaryReason.UNKNOWN_EVIDENCE;
		}
		return EvidenceRuntimeSummaryReason.UNKNOWN;
	}

	private boolean auditTrusted(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return pipelineResult.observableRuntimeResult()
				.summaryResponse()
				.summary()
				.auditTrusted();
	}

	private EvidenceCompleteness evidenceCompleteness(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return pipelineResult.observableRuntimeResult()
				.assessmentPipelineResult()
				.evidenceCorrelation()
				.completeness();
	}

	private boolean paymentInconsistencyDetected(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return pipelineResult.observableRuntimeResult()
				.summaryResponse()
				.summary()
				.risk() == OperationalUncertainty.CRITICAL
				&& pipelineResult.observableRuntimeResult()
						.summaryResponse()
						.summary()
						.uncertaintyReason()
				== ReliabilityLifecycleSummaryReason.PAYMENT_INCONSISTENCY_DETECTED;
	}

	private boolean paymentSafetyUncertain(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return pipelineResult.dispatchExecutionPipelineResult().paymentSafetyUncertain()
				|| pipelineResult.observableRuntimeResult()
						.assessmentPipelineResult()
						.evidenceCorrelation()
						.paymentSafetyUncertain();
	}

	private boolean contradictoryEvidenceDetected(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return pipelineResult.observableRuntimeResult()
				.collectionResult()
				.contradictionMarkerPresent()
				|| pipelineResult.observableRuntimeResult()
						.assessmentPipelineResult()
						.evidenceCorrelation()
						.contradictoryEvidence();
	}

	private boolean adapterFailureDetected(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return pipelineResult.dispatchExecutionPipelineResult()
				.propagatedCollectionStatus() == EvidenceCollectionStatus.FAILED;
	}

	private boolean partialEvidenceDetected(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return pipelineResult.dispatchExecutionPipelineResult()
				.propagatedCollectionStatus() == EvidenceCollectionStatus.PARTIAL;
	}

	private boolean unknownEvidenceDetected(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return pipelineResult.dispatchExecutionPipelineResult()
				.propagatedCollectionStatus() == EvidenceCollectionStatus.UNKNOWN
				|| pipelineResult.dispatchExecutionPipelineResult()
						.executionResponse() == null
				|| pipelineResult.dispatchExecutionPipelineResult()
						.executionResponse()
						.status() == EvidenceDispatchExecutionStatus.UNCERTAIN;
	}

	private boolean observabilityUnavailable(
			EvidenceExecutionObservablePipelineResult pipelineResult
	) {
		return pipelineResult.dispatchExecutionPipelineResult()
				.propagatedCollectionStatus() == EvidenceCollectionStatus.ABSENT;
	}
}
