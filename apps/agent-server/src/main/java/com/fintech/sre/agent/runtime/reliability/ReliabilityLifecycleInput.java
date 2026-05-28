package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ReliabilityLifecycleInput(
		ReliabilityAssessmentResult assessmentResult,
		ActionAdmissionDecision actionAdmissionDecision,
		ExecutionReadinessDecision executionReadinessDecision,
		ExecutorResponse executorResponse,
		PostExecutionVerificationDecision postExecutionVerificationDecision,
		PostExecutionConvergenceDecision postExecutionConvergenceDecision,
		PostExecutionRegressionDecision postExecutionRegressionDecision
) {
	public ReliabilityLifecycleInput {
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
	}
}
