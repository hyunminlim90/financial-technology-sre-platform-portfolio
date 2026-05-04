package com.fintech.sre.agent.actionlog.entity;

import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record ExecutedActionEntity(
		Long id,
		String incidentId,
		String recommendationId,
		Long recommendationActionId,
		String action,
		String executedBy,
		Instant executedAt,
		String executionMethod,
		String executionDetail,
		String expectedEffect,
		String actualEffect,
		String rollbackPlan,
		Boolean rollbackExecuted,
		Instant createdAt
) {
}
