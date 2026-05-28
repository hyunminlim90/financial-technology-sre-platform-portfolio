package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ExecutorRequest(
		ExecutionReadinessDecision readinessDecision,
		String auditTraceId
) {
	public ExecutorRequest {
		Objects.requireNonNull(
				readinessDecision,
				"readinessDecision must not be null"
		);
		if (!readinessDecision.ready()) {
			throw new IllegalArgumentException(
					"executor request requires accepted execution readiness"
			);
		}
		if (readinessDecision.requirement().paymentImpacting()
				&& (auditTraceId == null || auditTraceId.isBlank())) {
			throw new IllegalArgumentException(
					"payment-impacting executor request requires audit trace id"
			);
		}
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
