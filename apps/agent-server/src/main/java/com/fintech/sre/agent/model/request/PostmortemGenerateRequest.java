package com.fintech.sre.agent.model.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostmortemGenerateRequest(
		@NotBlank String incidentId,
		@NotBlank String alertName,
		@NotBlank String service,
		@NotBlank String environment,
		@NotNull Instant startTime,
		@NotNull Instant endTime,
		MetricsSnapshot metricsSnapshot,
		List<LogSample> logsSample,
		List<String> traceIds,
		List<ExecutedAction> executedActions,
		List<RecommendationHistory> recommendationHistory,
		String operatorSummary
) {
}
