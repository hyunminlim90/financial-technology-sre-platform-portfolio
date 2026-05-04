package com.fintech.sre.agent.actionlog.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecordExecutedActionRequest(
		String recommendationId,
		Long recommendationActionId,
		@NotBlank String action,
		@NotBlank String executedBy,
		@NotNull Instant executedAt,
		String executionMethod,
		String executionDetail,
		String expectedEffect,
		String actualEffect,
		String rollbackPlan
) {
}
