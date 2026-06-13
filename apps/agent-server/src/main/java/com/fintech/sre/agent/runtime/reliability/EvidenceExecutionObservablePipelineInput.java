package com.fintech.sre.agent.runtime.reliability;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record EvidenceExecutionObservablePipelineInput(
		EvidenceDispatchExecutionPipelineResult dispatchExecutionPipelineResult,
		String subjectId,
		Instant from,
		Instant to,
		RuntimeState runtimeState,
		PropagationSignal propagationSignal,
		boolean propagationActive,
		boolean rollbackRecentlyApplied,
		ConvergenceWindow convergenceWindow,
		List<RegressionSignal> regressionSignals,
		ScenarioReference scenarioReference,
		RollbackReference rollbackReference,
		VerificationReference verificationReference,
		boolean approvalProvided,
		boolean explicitApprovalProvided,
		boolean paymentSafetyAction,
		boolean unrestrictedRequested,
		boolean explicitExecutionAuthorized,
		boolean approvalCompleted,
		boolean rollbackReviewCompleted,
		boolean verificationReviewCompleted,
		LifecycleAuditDecision lifecycleAuditDecision
) {
	public EvidenceExecutionObservablePipelineInput {
		Objects.requireNonNull(
				dispatchExecutionPipelineResult,
				"dispatchExecutionPipelineResult must not be null"
		);
		Objects.requireNonNull(subjectId, "subjectId must not be null");
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(to, "to must not be null");
		Objects.requireNonNull(runtimeState, "runtimeState must not be null");
		Objects.requireNonNull(
				propagationSignal,
				"propagationSignal must not be null"
		);
		Objects.requireNonNull(
				convergenceWindow,
				"convergenceWindow must not be null"
		);
		Objects.requireNonNull(
				regressionSignals,
				"regressionSignals must not be null"
		);
		Objects.requireNonNull(
				lifecycleAuditDecision,
				"lifecycleAuditDecision must not be null"
		);
		if (subjectId.isBlank()) {
			throw new IllegalArgumentException("subjectId must not be blank");
		}
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("from must not be after to");
		}
		regressionSignals = List.copyOf(regressionSignals);
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
