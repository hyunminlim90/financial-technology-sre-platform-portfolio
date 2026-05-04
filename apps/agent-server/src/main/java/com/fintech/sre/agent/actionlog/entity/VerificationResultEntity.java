package com.fintech.sre.agent.actionlog.entity;

import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record VerificationResultEntity(
		Long id,
		String incidentId,
		Long executedActionId,
		String metricName,
		String query,
		Double beforeValue,
		Double afterValue,
		String expectedCondition,
		String status,
		Instant checkedAt
) {
}
