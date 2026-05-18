package com.fintech.sre.agent.recommendation.execution.result;

import java.time.Instant;
import java.util.Map;

public record HumanExecutionResultRecord(
		String executionResultId,
		String executionPlanId,
		String recommendationRecordId,
		String incidentId,
		HumanExecutionStatus status,
		String operatorId,
		String summary,
		Instant startedAt,
		Instant finishedAt,
		Instant recordedAt,
		Map<String, String> metadata
) {
	public HumanExecutionResultRecord {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
