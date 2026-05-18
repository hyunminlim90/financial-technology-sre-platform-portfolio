package com.fintech.sre.agent.persistence.r2dbc;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;

@Component
@Profile("r2dbc")
public class IncidentLifecycleEntityMapper {

	private final ObjectMapper objectMapper;

	public IncidentLifecycleEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public IncidentLifecycleEntity toEntity(
			IncidentLifecycleRecord record
	) {
		IncidentLifecycleEntity entity = new IncidentLifecycleEntity();
		entity.setIncidentLifecycleId(record.incidentLifecycleId());
		entity.setIncidentId(record.incidentId());
		entity.setPreviousStatus(record.previousStatus() == null
				? null
				: record.previousStatus().name());
		entity.setCurrentStatus(record.currentStatus() == null
				? null
				: record.currentStatus().name());
		entity.setTransitionReason(record.transitionReason() == null
				? null
				: record.transitionReason().name());
		entity.setOperatorId(record.operatorId());
		entity.setSummary(record.summary());
		entity.setTransitionedAt(record.transitionedAt());
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitize(record.metadata()),
				"Failed to serialize incident lifecycle metadata."
		));
		return entity;
	}

	public IncidentLifecycleRecord toDomain(
			IncidentLifecycleEntity entity
	) {
		return new IncidentLifecycleRecord(
				entity.getIncidentLifecycleId(),
				entity.getIncidentId(),
				entity.getPreviousStatus() == null
						? null
						: IncidentStatus.valueOf(entity.getPreviousStatus()),
				entity.getCurrentStatus() == null
						? null
						: IncidentStatus.valueOf(entity.getCurrentStatus()),
				entity.getTransitionReason() == null
						? null
						: IncidentTransitionReason.valueOf(entity.getTransitionReason()),
				entity.getOperatorId(),
				entity.getSummary(),
				entity.getTransitionedAt(),
				sanitize(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize incident lifecycle metadata."
				))
		);
	}

	private Map<String, String> sanitize(Map<String, String> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return Map.of();
		}

		return metadata.entrySet().stream()
				.filter(entry -> allowed(entry.getKey()))
				.collect(Collectors.toUnmodifiableMap(
						Map.Entry::getKey,
						Map.Entry::getValue
				));
	}

	private boolean allowed(String key) {
		if (key == null) {
			return false;
		}

		String lower = key.toLowerCase();
		return !lower.contains("payload")
				&& !lower.contains("customer")
				&& !lower.contains("secret")
				&& !lower.contains("token")
				&& !lower.contains("password")
				&& !lower.contains("payment")
				&& !lower.contains("rawlog")
				&& !lower.contains("prompt");
	}
}
