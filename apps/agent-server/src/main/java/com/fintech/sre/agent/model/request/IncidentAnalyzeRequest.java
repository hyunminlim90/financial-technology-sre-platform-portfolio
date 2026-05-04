package com.fintech.sre.agent.model.request;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncidentAnalyzeRequest(
		@NotBlank String incidentId,
		@NotBlank String alertName,
		@NotBlank String service,
		@NotBlank String environment,
		String severityHint,
		@NotNull Instant occurredAt,
		Map<String, String> labels,
		@Valid MetricsSnapshot metricsSnapshot,
		List<@Valid LogSample> logsSample,
		List<String> traceIds,
		@Valid DeploymentInfo deploymentInfo,
		String operatorNote
) {
}
