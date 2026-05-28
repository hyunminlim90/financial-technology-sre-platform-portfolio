package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ExecutorResponse(
		ExecutorStatus status,
		String executionId,
		String summary
) {
	public ExecutorResponse {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(summary, "summary must not be null");
	}

	public boolean converged() {
		return false;
	}

	public boolean requiresVerificationOrRollbackPath() {
		return status == ExecutorStatus.FAILURE;
	}
}
