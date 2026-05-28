package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record ReliabilityLifecycleResult(
		List<ReliabilityLifecycleStage> stages,
		ReliabilityAssessmentResult assessmentResult,
		ActionAdmissionDecision actionAdmissionDecision,
		ExecutionReadinessDecision executionReadinessDecision,
		ExecutorResponse executorResponse,
		PostExecutionVerificationDecision postExecutionVerificationDecision,
		PostExecutionConvergenceDecision postExecutionConvergenceDecision,
		PostExecutionRegressionDecision postExecutionRegressionDecision,
		RuntimeState lifecycleState,
		OperationalUncertainty overallRisk,
		ReliabilityLifecycleRejectionReason rejectionReason
) {
	public ReliabilityLifecycleResult {
		Objects.requireNonNull(stages, "stages must not be null");
		Objects.requireNonNull(assessmentResult, "assessmentResult must not be null");
		Objects.requireNonNull(
				actionAdmissionDecision,
				"actionAdmissionDecision must not be null"
		);
		Objects.requireNonNull(
				executionReadinessDecision,
				"executionReadinessDecision must not be null"
		);
		Objects.requireNonNull(executorResponse, "executorResponse must not be null");
		Objects.requireNonNull(
				postExecutionVerificationDecision,
				"postExecutionVerificationDecision must not be null"
		);
		Objects.requireNonNull(
				postExecutionConvergenceDecision,
				"postExecutionConvergenceDecision must not be null"
		);
		Objects.requireNonNull(
				postExecutionRegressionDecision,
				"postExecutionRegressionDecision must not be null"
		);
		Objects.requireNonNull(lifecycleState, "lifecycleState must not be null");
		Objects.requireNonNull(overallRisk, "overallRisk must not be null");
		stages = List.copyOf(stages);
	}

	public boolean stable() {
		return lifecycleState == RuntimeState.CONVERGED
				&& postExecutionVerificationDecision.verified()
				&& postExecutionConvergenceDecision.converged()
				&& !postExecutionRegressionDecision.regressionDetected();
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}
}
