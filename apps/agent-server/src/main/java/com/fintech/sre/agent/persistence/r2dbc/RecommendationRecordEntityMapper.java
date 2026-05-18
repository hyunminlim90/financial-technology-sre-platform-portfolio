package com.fintech.sre.agent.persistence.r2dbc;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

@Component
public class RecommendationRecordEntityMapper {

	private final ObjectMapper objectMapper;

	public RecommendationRecordEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public RecommendationRecordEntity toEntity(RecommendationRecord record) {
		RecommendationRecordEntity entity = new RecommendationRecordEntity();
		entity.setRecommendationRecordId(record.recommendationRecordId());
		entity.setIncidentId(record.incidentId());
		entity.setAuditId(record.auditId());
		entity.setSource(record.source());
		entity.setService(record.service());
		entity.setDomain(record.domain());
		entity.setSeverity(record.severity());
		entity.setStatus(record.status());
		entity.setGeneratedAt(record.generatedAt());
		entity.setRecommendedActionCount(record.recommendedActionCount());
		entity.setForbiddenActionCount(record.forbiddenActionCount());
		entity.setPolicyDecision(record.policyDecision());
		entity.setGuardrailDecision(record.guardrailDecision());
		entity.setActionTypesJson(JsonUtils.toJsonArray(
				objectMapper,
				record.actionTypes(),
				"Failed to serialize recommendation action list."
		));
		entity.setBlockedReasonsJson(JsonUtils.toJsonArray(
				objectMapper,
				record.blockedReasons(),
				"Failed to serialize recommendation blocked reasons."
		));
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitizeMetadata(record.metadata()),
				"Failed to serialize recommendation metadata."
		));
		return entity;
	}

	public RecommendationRecord toRecord(RecommendationRecordEntity entity) {
		return new RecommendationRecord(
				entity.getRecommendationRecordId(),
				entity.getIncidentId(),
				entity.getAuditId(),
				entity.getSource(),
				entity.getService(),
				entity.getDomain(),
				entity.getSeverity(),
				entity.getStatus(),
				entity.getGeneratedAt(),
				entity.getRecommendedActionCount(),
				entity.getForbiddenActionCount(),
				entity.getPolicyDecision(),
				entity.getGuardrailDecision(),
				JsonUtils.toStringList(
						objectMapper,
						entity.getActionTypesJson(),
						"Failed to deserialize recommendation action list."
				),
				JsonUtils.toStringList(
						objectMapper,
						entity.getBlockedReasonsJson(),
						"Failed to deserialize recommendation blocked reasons."
				),
				sanitizeMetadata(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize recommendation metadata."
				))
		);
	}

	private Map<String, String> sanitizeMetadata(Map<String, String> metadata) {
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
