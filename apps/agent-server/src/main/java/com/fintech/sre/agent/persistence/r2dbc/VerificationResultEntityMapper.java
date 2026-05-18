package com.fintech.sre.agent.persistence.r2dbc;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

@Component
@Profile("r2dbc")
public class VerificationResultEntityMapper {

	private final ObjectMapper objectMapper;

	public VerificationResultEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public VerificationResultEntity toEntity(
			VerificationResultRecord record
	) {
		VerificationResultEntity entity = new VerificationResultEntity();
		entity.setVerificationResultId(record.verificationResultId());
		entity.setExecutionResultId(record.executionResultId());
		entity.setExecutionPlanId(record.executionPlanId());
		entity.setRecommendationRecordId(record.recommendationRecordId());
		entity.setIncidentId(record.incidentId());
		entity.setStatus(record.status() == null ? null : record.status().name());
		entity.setOperatorId(record.operatorId());
		entity.setSummary(record.summary());
		entity.setVerifiedAt(record.verifiedAt());
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitize(record.metadata()),
				"Failed to serialize verification result metadata."
		));
		return entity;
	}

	public VerificationResultRecord toDomain(
			VerificationResultEntity entity
	) {
		return new VerificationResultRecord(
				entity.getVerificationResultId(),
				entity.getExecutionResultId(),
				entity.getExecutionPlanId(),
				entity.getRecommendationRecordId(),
				entity.getIncidentId(),
				entity.getStatus() == null
						? null
						: VerificationStatus.valueOf(entity.getStatus()),
				entity.getOperatorId(),
				entity.getSummary(),
				entity.getVerifiedAt(),
				sanitize(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize verification result metadata."
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
