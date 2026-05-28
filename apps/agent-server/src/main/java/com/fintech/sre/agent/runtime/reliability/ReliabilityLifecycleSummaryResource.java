package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ReliabilityLifecycleSummaryResource {

	public ReliabilityLifecycleSummaryResponse view(
			ReliabilityLifecycleSummary summary
	) {
		Objects.requireNonNull(summary, "summary must not be null");

		return new ReliabilityLifecycleSummaryResponse(
				new ReliabilityLifecycleSummaryView(
						viewStatus(summary.status()),
						summary.trusted(),
						summary.risk(),
						regressionDetected(summary.reason()),
						summary.reason()
				)
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private ReliabilityLifecycleSummaryViewStatus viewStatus(
			ReliabilityLifecycleSummaryStatus status
	) {
		return switch (status) {
			case STABLE -> ReliabilityLifecycleSummaryViewStatus.STABLE;
			case RECOVERED -> ReliabilityLifecycleSummaryViewStatus.RECOVERED;
			case UNCERTAIN -> ReliabilityLifecycleSummaryViewStatus.UNCERTAIN;
			case FAILED -> ReliabilityLifecycleSummaryViewStatus.FAILED;
		};
	}

	private boolean regressionDetected(
			ReliabilityLifecycleSummaryReason reason
	) {
		return reason == ReliabilityLifecycleSummaryReason.REGRESSION_DETECTED
				|| reason
				== ReliabilityLifecycleSummaryReason.PAYMENT_INCONSISTENCY_DETECTED;
	}
}
