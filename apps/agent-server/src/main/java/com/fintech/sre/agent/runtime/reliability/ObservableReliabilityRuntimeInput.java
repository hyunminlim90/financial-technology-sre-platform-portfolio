package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record ObservableReliabilityRuntimeInput(
		List<EvidenceAdapterPort> adapters,
		List<EvidenceQuery> queries,
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
	public ObservableReliabilityRuntimeInput {
		Objects.requireNonNull(adapters, "adapters must not be null");
		Objects.requireNonNull(queries, "queries must not be null");
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
		adapters = List.copyOf(adapters);
		queries = List.copyOf(queries);
		regressionSignals = List.copyOf(regressionSignals);
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
