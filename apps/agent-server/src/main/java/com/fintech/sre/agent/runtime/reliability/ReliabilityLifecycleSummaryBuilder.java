package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ReliabilityLifecycleSummaryBuilder {

	public ReliabilityLifecycleSummary build(
			ReliabilityLifecycleResult lifecycleResult,
			LifecycleAuditDecision lifecycleAuditDecision
	) {
		Objects.requireNonNull(lifecycleResult, "lifecycleResult must not be null");
		Objects.requireNonNull(
				lifecycleAuditDecision,
				"lifecycleAuditDecision must not be null"
		);

		return new ReliabilityLifecycleSummary(
				status(lifecycleResult, lifecycleAuditDecision),
				ReliabilityLifecycleSummaryScope.OPERATOR_READ_MODEL,
				lifecycleAuditDecision.lifecycleTrustworthy(),
				risk(lifecycleResult),
				lifecycleResult.lifecycleState(),
				reason(lifecycleResult, lifecycleAuditDecision)
		);
	}

	private ReliabilityLifecycleSummaryStatus status(
			ReliabilityLifecycleResult lifecycleResult,
			LifecycleAuditDecision lifecycleAuditDecision
	) {
		if (lifecycleResult.executorResponse().status() == ExecutorStatus.FAILURE) {
			return ReliabilityLifecycleSummaryStatus.FAILED;
		}
		if (stableCandidate(lifecycleResult, lifecycleAuditDecision)) {
			return ReliabilityLifecycleSummaryStatus.STABLE;
		}
		if (lifecycleResult.postExecutionVerificationDecision().verified()
				&& !lifecycleResult.postExecutionRegressionDecision()
						.regressionDetected()) {
			return ReliabilityLifecycleSummaryStatus.RECOVERED;
		}
		return ReliabilityLifecycleSummaryStatus.UNCERTAIN;
	}

	private ReliabilityLifecycleSummaryReason reason(
			ReliabilityLifecycleResult lifecycleResult,
			LifecycleAuditDecision lifecycleAuditDecision
	) {
		if (paymentInconsistencyDetected(lifecycleResult)) {
			return ReliabilityLifecycleSummaryReason.PAYMENT_INCONSISTENCY_DETECTED;
		}
		if (lifecycleResult.postExecutionRegressionDecision().regressionDetected()) {
			return ReliabilityLifecycleSummaryReason.REGRESSION_DETECTED;
		}
		if (lifecycleResult.executorResponse().status() == ExecutorStatus.FAILURE) {
			return ReliabilityLifecycleSummaryReason.EXECUTOR_RESPONSE_FAILED;
		}
		if (lifecycleResult.executorResponse().status() == ExecutorStatus.UNKNOWN) {
			return ReliabilityLifecycleSummaryReason.EXECUTOR_RESPONSE_UNKNOWN;
		}
		if (!lifecycleAuditDecision.lifecycleTrustworthy()) {
			return ReliabilityLifecycleSummaryReason.AUDIT_INTEGRITY_INCOMPLETE;
		}
		if (stableCandidate(lifecycleResult, lifecycleAuditDecision)) {
			return ReliabilityLifecycleSummaryReason.STABLE_CONVERGENCE_CONFIRMED;
		}
		if (lifecycleResult.postExecutionVerificationDecision().verified()) {
			return ReliabilityLifecycleSummaryReason
					.POST_EXECUTION_VERIFICATION_CONFIRMED;
		}
		return ReliabilityLifecycleSummaryReason.EXECUTION_ACKNOWLEDGEMENT_ONLY;
	}

	private OperationalUncertainty risk(
			ReliabilityLifecycleResult lifecycleResult
	) {
		if (paymentInconsistencyDetected(lifecycleResult)) {
			return OperationalUncertainty.CRITICAL;
		}
		return lifecycleResult.overallRisk();
	}

	private boolean paymentInconsistencyDetected(
			ReliabilityLifecycleResult lifecycleResult
	) {
		return lifecycleResult.postExecutionRegressionDecision().regressionDetected()
				&& lifecycleResult.postExecutionRegressionDecision().requirement().signals()
						.stream()
						.anyMatch(signal -> signal.type()
								== RegressionSignalType.PAYMENT_INCONSISTENCY);
	}

	private boolean stableCandidate(
			ReliabilityLifecycleResult lifecycleResult,
			LifecycleAuditDecision lifecycleAuditDecision
	) {
		return lifecycleResult.stable()
				&& lifecycleAuditDecision.lifecycleTrustworthy();
	}
}
