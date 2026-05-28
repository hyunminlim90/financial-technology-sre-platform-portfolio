package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record PostExecutionRegressionDecision(
		PostExecutionRegressionStatus status,
		PostExecutionRegressionRequirement requirement,
		RegressionSeverity severity,
		OperationalUncertainty uncertainty,
		PostExecutionRegressionRejectionReason rejectionReason
) {
	public PostExecutionRegressionDecision {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(requirement, "requirement must not be null");
		Objects.requireNonNull(severity, "severity must not be null");
		Objects.requireNonNull(uncertainty, "uncertainty must not be null");
		if (status == PostExecutionRegressionStatus.DETECTED
				&& rejectionReason != null) {
			throw new IllegalArgumentException(
					"detected post execution regression must not contain rejection reason"
			);
		}
		if (status != PostExecutionRegressionStatus.DETECTED
				&& rejectionReason == null) {
			throw new IllegalArgumentException(
					"non-detected post execution regression requires rejection reason"
			);
		}
	}

	public boolean regressionDetected() {
		return status == PostExecutionRegressionStatus.DETECTED;
	}

	public boolean semanticDetectionOnly() {
		return true;
	}
}
