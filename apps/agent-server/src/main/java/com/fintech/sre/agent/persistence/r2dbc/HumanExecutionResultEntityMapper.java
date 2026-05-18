package com.fintech.sre.agent.persistence.r2dbc;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;

@Component
@Profile("r2dbc")
public class HumanExecutionResultEntityMapper {

	private final ObjectMapper objectMapper;

	public HumanExecutionResultEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public HumanExecutionResultEntity toEntity(
			HumanExecutionResultRecord record
	) {
		HumanExecutionResultEntity entity = new HumanExecutionResultEntity();
		entity.setExecutionResultId(record.executionResultId());
		entity.setExecutionPlanId(record.executionPlanId());
		entity.setRecommendationRecordId(record.recommendationRecordId());
		entity.setIncidentId(record.incidentId());
		entity.setStatus(record.status() == null ? null : record.status().name());
		entity.setOperatorId(record.operatorId());
		entity.setSummary(record.summary());
		entity.setStartedAt(record.startedAt());
		entity.setFinishedAt(record.finishedAt());
		entity.setRecordedAt(record.recordedAt());
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitize(record.metadata()),
				"Failed to serialize human execution result metadata."
		));
		return entity;
	}

	public HumanExecutionResultRecord toDomain(
			HumanExecutionResultEntity entity
	) {
		return new HumanExecutionResultRecord(
				entity.getExecutionResultId(),
				entity.getExecutionPlanId(),
				entity.getRecommendationRecordId(),
				entity.getIncidentId(),
				entity.getStatus() == null
						? null
						: HumanExecutionStatus.valueOf(entity.getStatus()),
				entity.getOperatorId(),
				entity.getSummary(),
				entity.getStartedAt(),
				entity.getFinishedAt(),
				entity.getRecordedAt(),
				sanitize(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize human execution result metadata."
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
