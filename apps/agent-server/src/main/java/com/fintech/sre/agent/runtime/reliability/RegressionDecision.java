package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record RegressionDecision(
		boolean regressionDetected,
		RegressionSeverity severity,
		RegressionAssessment assessment,
		RegressionRejectionReason rejectionReason
) {
	public RegressionDecision {
		Objects.requireNonNull(severity, "severity must not be null");
		Objects.requireNonNull(assessment, "assessment must not be null");
		if (regressionDetected && rejectionReason != null) {
			throw new IllegalArgumentException(
					"detected regression decision must not contain rejection reason"
			);
		}
		if (!regressionDetected && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected regression decision requires rejection reason"
			);
		}
	}

	public static RegressionDecision evaluate(
			RegressionAssessment assessment
	) {
		Objects.requireNonNull(assessment, "assessment must not be null");

		if (!assessment.postConvergence()) {
			return rejected(
					assessment,
					RegressionRejectionReason.NOT_POST_CONVERGENCE_STATE
			);
		}
		if (!assessment.hasSignals()) {
			return rejected(
					assessment,
					RegressionRejectionReason.NO_REGRESSION_SIGNALS
			);
		}

		return new RegressionDecision(
				true,
				severityOf(assessment),
				assessment,
				null
		);
	}

	private static RegressionDecision rejected(
			RegressionAssessment assessment,
			RegressionRejectionReason rejectionReason
	) {
		return new RegressionDecision(
				false,
				RegressionSeverity.LOW,
				assessment,
				rejectionReason
		);
	}

	private static RegressionSeverity severityOf(
			RegressionAssessment assessment
	) {
		if (assessment.signals().stream()
				.anyMatch(signal -> signal.type() == RegressionSignalType.PAYMENT_SAFETY
						|| signal.type()
						== RegressionSignalType.PAYMENT_INCONSISTENCY)) {
			return RegressionSeverity.HIGH;
		}
		if (assessment.signals().stream()
				.anyMatch(signal -> signal.type()
						== RegressionSignalType.PROPAGATION_REACTIVATED)
				|| assessment.signals().stream()
						.anyMatch(signal -> signal.type()
								== RegressionSignalType.CONTRADICTORY_EVIDENCE)) {
			return RegressionSeverity.HIGH;
		}
		return RegressionSeverity.MODERATE;
	}

	public boolean semanticDetectionOnly() {
		return true;
	}
}
