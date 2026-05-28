package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public class ReliabilityLifecycleOrchestrator {

	public ReliabilityLifecycleResult orchestrate(
			ReliabilityLifecycleInput input
	) {
		Objects.requireNonNull(input, "input must not be null");

		return new ReliabilityLifecycleResult(
				List.of(
						ReliabilityLifecycleStage.PRE_EXECUTION_ASSESSMENT,
						ReliabilityLifecycleStage.ACTION_ADMISSION,
						ReliabilityLifecycleStage.EXECUTION_READINESS,
						ReliabilityLifecycleStage.EXECUTOR_RESPONSE,
						ReliabilityLifecycleStage.POST_EXECUTION_VERIFICATION,
						ReliabilityLifecycleStage.POST_EXECUTION_CONVERGENCE,
						ReliabilityLifecycleStage.POST_EXECUTION_REGRESSION
				),
				input.assessmentResult(),
				input.actionAdmissionDecision(),
				input.executionReadinessDecision(),
				input.executorResponse(),
				input.postExecutionVerificationDecision(),
				input.postExecutionConvergenceDecision(),
				input.postExecutionRegressionDecision(),
				lifecycleState(input),
				overallRisk(input),
				rejectionReason(input)
		);
	}

	private RuntimeState lifecycleState(ReliabilityLifecycleInput input) {
		if (input.assessmentResult().terminal()
				|| input.executorResponse().status() == ExecutorStatus.FAILURE) {
			return RuntimeState.FAILED;
		}
		if (input.postExecutionRegressionDecision().regressionDetected()) {
			return RuntimeState.DEGRADED;
		}
		if (input.postExecutionConvergenceDecision().converged()
				&& input.postExecutionVerificationDecision().verified()) {
			return RuntimeState.CONVERGED;
		}
		if (input.postExecutionVerificationDecision().verified()) {
			return RuntimeState.VERIFIED;
		}
		if (input.executorResponse().status() == ExecutorStatus.UNKNOWN) {
			return RuntimeState.UNSTABLE;
		}
		if (input.executorResponse().status() == ExecutorStatus.SUCCESS) {
			return RuntimeState.VERIFYING;
		}
		return input.assessmentResult().runtimeState();
	}

	private ReliabilityLifecycleRejectionReason rejectionReason(
			ReliabilityLifecycleInput input
	) {
		if (!input.actionAdmissionDecision().admitted()) {
			return ReliabilityLifecycleRejectionReason.ACTION_ADMISSION_REJECTED;
		}
		if (!input.executionReadinessDecision().ready()) {
			return ReliabilityLifecycleRejectionReason.EXECUTION_READINESS_REJECTED;
		}
		if (input.executorResponse().status() == ExecutorStatus.FAILURE) {
			return ReliabilityLifecycleRejectionReason.EXECUTOR_RESPONSE_FAILED;
		}
		if (input.executorResponse().status() == ExecutorStatus.UNKNOWN) {
			return ReliabilityLifecycleRejectionReason.EXECUTOR_RESPONSE_UNKNOWN;
		}
		if (input.postExecutionConvergenceDecision().converged()
				&& !input.postExecutionVerificationDecision().verified()) {
			return ReliabilityLifecycleRejectionReason
					.CONVERGENCE_REQUIRES_POST_EXECUTION_VERIFICATION;
		}
		if (!input.postExecutionVerificationDecision().verified()) {
			return ReliabilityLifecycleRejectionReason
					.POST_EXECUTION_VERIFICATION_REQUIRED;
		}
		if (!input.postExecutionConvergenceDecision().converged()) {
			return ReliabilityLifecycleRejectionReason
					.POST_EXECUTION_CONVERGENCE_REQUIRED;
		}
		if (input.postExecutionRegressionDecision().regressionDetected()) {
			return ReliabilityLifecycleRejectionReason
					.POST_EXECUTION_REGRESSION_DETECTED;
		}
		if (input.assessmentResult().rejectionReason() != null) {
			return ReliabilityLifecycleRejectionReason
					.PRE_EXECUTION_ASSESSMENT_REJECTED;
		}
		return null;
	}

	private OperationalUncertainty overallRisk(ReliabilityLifecycleInput input) {
		OperationalUncertainty risk = input.assessmentResult().overallRisk();

		if (input.executorResponse().status() == ExecutorStatus.FAILURE) {
			risk = max(risk, OperationalUncertainty.HIGH);
		}
		if (input.executorResponse().status() == ExecutorStatus.UNKNOWN) {
			risk = max(risk, OperationalUncertainty.HIGH);
		}

		risk = max(risk, input.postExecutionRegressionDecision().uncertainty());
		return risk;
	}

	private OperationalUncertainty max(
			OperationalUncertainty left,
			OperationalUncertainty right
	) {
		return left.ordinal() >= right.ordinal() ? left : right;
	}
}
