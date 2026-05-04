package com.fintech.sre.agent.actionlog.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecordVerificationRequest(
		@NotBlank String metricName,
		String query,
		Double beforeValue,
		Double afterValue,
		String expectedCondition,
		@NotBlank String status,
		Instant checkedAt
) {
}
