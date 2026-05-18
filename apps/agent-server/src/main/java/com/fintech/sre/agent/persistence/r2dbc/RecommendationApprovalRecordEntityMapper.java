package com.fintech.sre.agent.persistence.r2dbc;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;

@Component
@Profile("r2dbc")
public class RecommendationApprovalRecordEntityMapper {

	private final ObjectMapper objectMapper;

	public RecommendationApprovalRecordEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public RecommendationApprovalRecordEntity toEntity(
			RecommendationApprovalRecord record
	) {
		RecommendationApprovalRecordEntity entity =
				new RecommendationApprovalRecordEntity();
		entity.setApprovalId(record.approvalId());
		entity.setRecommendationRecordId(record.recommendationRecordId());
		entity.setIncidentId(record.incidentId());
		entity.setStatus(
				record.status() == null ? null : record.status().name()
		);
		entity.setOperatorId(record.operatorId());
		entity.setReason(record.reason());
		entity.setDecidedAt(record.decidedAt());
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitize(record.metadata()),
				"Failed to serialize recommendation approval metadata."
		));
		return entity;
	}

	public RecommendationApprovalRecord toDomain(
			RecommendationApprovalRecordEntity entity
	) {
		return new RecommendationApprovalRecord(
				entity.getApprovalId(),
				entity.getRecommendationRecordId(),
				entity.getIncidentId(),
				entity.getStatus() == null
						? null
						: RecommendationApprovalStatus.valueOf(entity.getStatus()),
				entity.getOperatorId(),
				entity.getReason(),
				entity.getDecidedAt(),
				sanitize(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize recommendation approval metadata."
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
