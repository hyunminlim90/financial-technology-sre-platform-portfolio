package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public class EvidenceExecutionObservablePipeline {

	private final ObservableReliabilityRuntimePipeline observableRuntimePipeline;

	public EvidenceExecutionObservablePipeline(
			ObservableReliabilityRuntimePipeline observableRuntimePipeline
	) {
		this.observableRuntimePipeline = Objects.requireNonNull(
				observableRuntimePipeline,
				"observableRuntimePipeline must not be null"
		);
	}

	public EvidenceExecutionObservablePipelineResult run(
			EvidenceExecutionObservablePipelineInput input
	) {
		Objects.requireNonNull(input, "input must not be null");

		ObservableReliabilityRuntimeResult observableRuntimeResult =
				observableRuntimePipeline.run(new ObservableReliabilityRuntimeInput(
						adapters(input.dispatchExecutionPipelineResult()),
						queries(input.dispatchExecutionPipelineResult(), input),
						input.runtimeState(),
						input.propagationSignal(),
						input.propagationActive(),
						input.rollbackRecentlyApplied(),
						input.convergenceWindow(),
						input.regressionSignals(),
						input.scenarioReference(),
						input.rollbackReference(),
						input.verificationReference(),
						input.approvalProvided(),
						input.explicitApprovalProvided(),
						input.paymentSafetyAction(),
						input.unrestrictedRequested(),
						input.explicitExecutionAuthorized(),
						input.approvalCompleted(),
						input.rollbackReviewCompleted(),
						input.verificationReviewCompleted(),
						input.lifecycleAuditDecision()
				));

		return new EvidenceExecutionObservablePipelineResult(
				List.of(
						EvidenceExecutionObservablePipelineStage
								.EVIDENCE_DISPATCH_EXECUTION,
						EvidenceExecutionObservablePipelineStage.OBSERVABLE_RUNTIME
				),
				input.dispatchExecutionPipelineResult(),
				observableRuntimeResult,
				rejectionReason(
						input.dispatchExecutionPipelineResult(),
						observableRuntimeResult
				)
		);
	}

	private List<EvidenceAdapterPort> adapters(
			EvidenceDispatchExecutionPipelineResult dispatchExecutionPipelineResult
	) {
		return evidenceResults(dispatchExecutionPipelineResult).stream()
				.map(result -> (EvidenceAdapterPort) query -> result)
				.toList();
	}

	private List<EvidenceQuery> queries(
			EvidenceDispatchExecutionPipelineResult dispatchExecutionPipelineResult,
			EvidenceExecutionObservablePipelineInput input
	) {
		return evidenceResults(dispatchExecutionPipelineResult).stream()
				.map(result -> new EvidenceQuery(
						result.sourceType(),
						input.subjectId(),
						input.from(),
						input.to(),
						input.dispatchExecutionPipelineResult()
								.dispatchResult()
								.request()
								.routingPlan()
								.paymentConsistencyRequired()
					))
				.toList();
	}

	private List<EvidenceQueryResult> evidenceResults(
			EvidenceDispatchExecutionPipelineResult dispatchExecutionPipelineResult
	) {
		if (dispatchExecutionPipelineResult.executionResponse() != null) {
			return dispatchExecutionPipelineResult.executionResponse().results();
		}
		return dispatchExecutionPipelineResult.dispatchResult().results();
	}

	private EvidenceExecutionObservablePipelineRejectionReason rejectionReason(
			EvidenceDispatchExecutionPipelineResult dispatchExecutionPipelineResult,
			ObservableReliabilityRuntimeResult observableRuntimeResult
	) {
		if (dispatchExecutionPipelineResult.rejectionReason() != null) {
			return EvidenceExecutionObservablePipelineRejectionReason
					.DISPATCH_EXECUTION_REJECTED;
		}
		if (observableRuntimeResult.rejectionReason() != null) {
			return EvidenceExecutionObservablePipelineRejectionReason
					.OBSERVABLE_RUNTIME_REJECTED;
		}
		return null;
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

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
