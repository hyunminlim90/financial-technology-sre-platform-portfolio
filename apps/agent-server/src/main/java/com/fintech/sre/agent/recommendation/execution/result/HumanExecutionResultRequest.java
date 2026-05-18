package com.fintech.sre.agent.recommendation.execution.result;

import java.time.Instant;
import java.util.Map;

public record HumanExecutionResultRequest(
		HumanExecutionStatus status,
		String operatorId,
		String summary,
		Instant startedAt,
		Instant finishedAt,
		Map<String, String> metadata
) {
}
