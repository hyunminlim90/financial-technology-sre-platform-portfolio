package com.fintech.sre.agent.model.request;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fintech.sre.agent.model.common.IncidentContext;

public record IncidentRecommendationRequest(
		String incidentId,
		String alertName,
		String service,
		String environment,
		String severityHint,
		Instant occurredAt,
		Map<String, String> labels,
		MetricsSnapshot metricsSnapshot,
		List<LogSample> logsSample,
		List<String> traceIds,
		DeploymentInfo deploymentInfo,
		String operatorNote
) {
	public static IncidentRecommendationRequest from(IncidentAnalyzeRequest request) {
		return new IncidentRecommendationRequest(
				request.incidentId(),
				request.alertName(),
				request.service(),
				request.environment(),
				request.severityHint(),
				request.occurredAt(),
				request.labels(),
				request.metricsSnapshot(),
				request.logsSample(),
				request.traceIds(),
				request.deploymentInfo(),
				request.operatorNote()
		);
	}

	public static IncidentRecommendationRequest from(IncidentContext context) {
		return new IncidentRecommendationRequest(
				context.incidentId(),
				context.alertName(),
				context.service(),
				context.environment(),
				context.severityHint(),
				context.occurredAt(),
				context.labels(),
				context.metricsSnapshot(),
				context.logsSample(),
				context.traceIds(),
				context.deploymentInfo(),
				context.operatorNote()
		);
	}
}
