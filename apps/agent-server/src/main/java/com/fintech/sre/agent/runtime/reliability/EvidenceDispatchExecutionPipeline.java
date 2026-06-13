package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public class EvidenceDispatchExecutionPipeline {

	private final EvidenceDispatchContract dispatchContract;
	private final EvidenceDispatchExecutorPort executorPort;

	public EvidenceDispatchExecutionPipeline(
			EvidenceDispatchContract dispatchContract,
			EvidenceDispatchExecutorPort executorPort
	) {
		this.dispatchContract = Objects.requireNonNull(
				dispatchContract,
				"dispatchContract must not be null"
		);
		this.executorPort = Objects.requireNonNull(
				executorPort,
				"executorPort must not be null"
		);
	}

	public EvidenceDispatchExecutionPipelineResult run(
			EvidenceDispatchExecutionPipelineInput input
	) {
		Objects.requireNonNull(input, "input must not be null");

		EvidenceDispatchResult dispatchResult = dispatchContract.dispatch(
				input.dispatchRequest()
		);
		if (dispatchResult.status() == EvidenceDispatchStatus.REJECTED) {
			return new EvidenceDispatchExecutionPipelineResult(
					List.of(EvidenceDispatchExecutionPipelineStage.DISPATCH),
					dispatchResult,
					null,
					null,
					EvidenceCollectionStatus.UNKNOWN,
					dispatchResult.request().routingPlan().paymentConsistencyRequired(),
					EvidenceDispatchExecutionPipelineRejectionReason.DISPATCH_REJECTED
			);
		}

		EvidenceDispatchExecutionRequest executionRequest;
		try {
			executionRequest = new EvidenceDispatchExecutionRequest(dispatchResult);
		} catch (IllegalArgumentException exception) {
			return new EvidenceDispatchExecutionPipelineResult(
					List.of(
							EvidenceDispatchExecutionPipelineStage.DISPATCH,
							EvidenceDispatchExecutionPipelineStage.EXECUTION_REQUEST
					),
					dispatchResult,
					null,
					null,
					EvidenceCollectionStatus.UNKNOWN,
					true,
					EvidenceDispatchExecutionPipelineRejectionReason
							.EXECUTION_REQUEST_REJECTED
			);
		}

		EvidenceDispatchExecutionResponse executionResponse = executorPort.execute(
				executionRequest
		);
		return new EvidenceDispatchExecutionPipelineResult(
				List.of(
						EvidenceDispatchExecutionPipelineStage.DISPATCH,
						EvidenceDispatchExecutionPipelineStage.EXECUTION_REQUEST,
						EvidenceDispatchExecutionPipelineStage.EXECUTION_RESPONSE
				),
				dispatchResult,
				executionRequest,
				executionResponse,
				propagatedCollectionStatus(executionResponse),
				paymentSafetyUncertain(dispatchResult, executionResponse),
				executionResponse.status() == EvidenceDispatchExecutionStatus.REJECTED
						? EvidenceDispatchExecutionPipelineRejectionReason
								.EXECUTION_RESPONSE_REJECTED
						: null
		);
	}

	private EvidenceCollectionStatus propagatedCollectionStatus(
			EvidenceDispatchExecutionResponse executionResponse
	) {
		if (executionResponse.results().stream()
				.anyMatch(result -> result.status() == EvidenceCollectionStatus.PARTIAL)) {
			return EvidenceCollectionStatus.PARTIAL;
		}
		if (executionResponse.results().stream()
				.anyMatch(result -> result.status() == EvidenceCollectionStatus.UNKNOWN)
				|| executionResponse.status() == EvidenceDispatchExecutionStatus.UNCERTAIN) {
			return EvidenceCollectionStatus.UNKNOWN;
		}
		if (executionResponse.results().stream()
				.anyMatch(result -> result.status() == EvidenceCollectionStatus.ABSENT)) {
			return EvidenceCollectionStatus.ABSENT;
		}
		return EvidenceCollectionStatus.COLLECTED;
	}

	private boolean paymentSafetyUncertain(
			EvidenceDispatchResult dispatchResult,
			EvidenceDispatchExecutionResponse executionResponse
	) {
		return dispatchResult.request().routingPlan().paymentConsistencyRequired()
				&& executionResponse.results().stream().noneMatch(
						EvidenceQueryResult::paymentConsistencyMetadataPresent
				);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendationAuthority() {
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
