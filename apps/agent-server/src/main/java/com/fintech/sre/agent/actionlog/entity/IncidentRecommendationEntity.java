package com.fintech.sre.agent.actionlog.entity;

import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record IncidentRecommendationEntity(
		Long id,
		String incidentId,
		String recommendationId,
		String alertName,
		String service,
		String environment,
		String failureMode,
		String severity,
		String impactScope,
		String confidenceLevel,
		String summary,
		String rawRequest,
		String rawResponse,
		Boolean humanApprovalRequired,
		Instant createdAt
) {
}
