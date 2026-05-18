package com.fintech.sre.agent.recommendation.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

@Component
public class RecommendationRecordMapper {

	private final RecommendationRecordIdGenerator idGenerator;

	public RecommendationRecordMapper(RecommendationRecordIdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}

	public RecommendationRecord toRecord(
			String auditId,
			AlertEvent alert,
			IncidentRecommendationResponse response
	) {
		return new RecommendationRecord(
				idGenerator.generate(),
				safeIncidentId(response, alert),
				auditId,
				alert == null || alert.source() == null ? "UNKNOWN" : alert.source().name(),
				alert == null ? "unknown" : safe(alert.service()),
				alert == null ? "unknown" : safe(alert.domain()),
				alert == null || alert.severity() == null ? "UNKNOWN" : alert.severity().name(),
				alert == null ? "unknown" : safe(alert.status()),
				Instant.now(),
				recommendedActionCount(response),
				forbiddenActionCount(response),
				policyDecision(response),
				guardrailDecision(response),
				actionTypes(response),
				blockedReasons(response),
				metadata(alert)
		);
	}

	private String safeIncidentId(IncidentRecommendationResponse response, AlertEvent alert) {
		if (response != null && response.incidentId() != null && !response.incidentId().isBlank()) {
			return response.incidentId();
		}

		if (alert != null && alert.alertId() != null && !alert.alertId().isBlank()) {
			return alert.alertId();
		}

		return "unknown";
	}

	private int recommendedActionCount(IncidentRecommendationResponse response) {
		return response == null || response.recommendedActions() == null
				? 0
				: response.recommendedActions().size();
	}

	private int forbiddenActionCount(IncidentRecommendationResponse response) {
		return response == null || response.forbiddenActions() == null
				? 0
				: response.forbiddenActions().size();
	}

	private String policyDecision(IncidentRecommendationResponse response) {
		return response == null || response.policyDecision() == null || response.policyDecision().decision() == null
				? "UNKNOWN"
				: response.policyDecision().decision();
	}

	private String guardrailDecision(IncidentRecommendationResponse response) {
		return response == null || response.guardrailDecision() == null
				? "UNKNOWN"
				: response.guardrailDecision();
	}

	private List<String> actionTypes(IncidentRecommendationResponse response) {
		if (response == null || response.recommendedActions() == null) {
			return List.of();
		}

		return response.recommendedActions().stream()
				.map(RecommendedAction::command)
				.filter(Objects::nonNull)
				.map(command -> command.type().name())
				.distinct()
				.toList();
	}

	private List<String> blockedReasons(IncidentRecommendationResponse response) {
		if (response == null || response.forbiddenActions() == null) {
			return List.of();
		}

		return response.forbiddenActions().stream()
				.map(action -> action.reason())
				.filter(reason -> reason != null && !reason.isBlank())
				.distinct()
				.toList();
	}

	private Map<String, String> metadata(AlertEvent alert) {
		if (alert == null) {
			return Map.of();
		}

		return Map.of(
				"alertId", safe(alert.alertId()),
				"alertName", safe(alert.alertName()),
				"namespace", safe(alert.namespace())
		);
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}
}
