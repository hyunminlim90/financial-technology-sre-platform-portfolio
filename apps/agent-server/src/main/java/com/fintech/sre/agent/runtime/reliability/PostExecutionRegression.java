package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class PostExecutionRegression {

	public PostExecutionRegressionDecision detect(
			PostExecutionRegressionRequirement requirement
	) {
		Objects.requireNonNull(requirement, "requirement must not be null");

		if (!requirement.postExecutionConverged()) {
			return rejected(
					requirement,
					PostExecutionRegressionRejectionReason
							.NOT_POST_EXECUTION_CONVERGED
			);
		}
		if (!requirement.hasSignals()) {
			return rejected(
					requirement,
					PostExecutionRegressionRejectionReason.NO_REGRESSION_SIGNALS
			);
		}

		return new PostExecutionRegressionDecision(
				PostExecutionRegressionStatus.DETECTED,
				requirement,
				severityOf(requirement),
				uncertaintyOf(requirement),
				null
		);
	}

	private PostExecutionRegressionDecision rejected(
			PostExecutionRegressionRequirement requirement,
			PostExecutionRegressionRejectionReason rejectionReason
	) {
		return new PostExecutionRegressionDecision(
				PostExecutionRegressionStatus.REJECTED,
				requirement,
				RegressionSeverity.LOW,
				OperationalUncertainty.LOW,
				rejectionReason
		);
	}

	private RegressionSeverity severityOf(
			PostExecutionRegressionRequirement requirement
	) {
		if (requirement.signals().stream().anyMatch(signal -> signal.type()
				== RegressionSignalType.PAYMENT_INCONSISTENCY
				|| signal.type() == RegressionSignalType.PAYMENT_SAFETY
				|| signal.type() == RegressionSignalType.PROPAGATION_REACTIVATED
				|| signal.type()
				== RegressionSignalType.CONTRADICTORY_EVIDENCE)) {
			return RegressionSeverity.HIGH;
		}
		return RegressionSeverity.MODERATE;
	}

	private OperationalUncertainty uncertaintyOf(
			PostExecutionRegressionRequirement requirement
	) {
		if (requirement.signals().stream().anyMatch(signal -> signal.type()
				== RegressionSignalType.PAYMENT_INCONSISTENCY)) {
			return OperationalUncertainty.CRITICAL;
		}
		if (requirement.signals().stream().anyMatch(signal -> signal.type()
				== RegressionSignalType.PAYMENT_SAFETY
				|| signal.type() == RegressionSignalType.PROPAGATION_REACTIVATED
				|| signal.type()
				== RegressionSignalType.CONTRADICTORY_EVIDENCE)) {
			return OperationalUncertainty.HIGH;
		}
		return OperationalUncertainty.MODERATE;
	}
}
