package com.fintech.sre.agent.persistence.r2dbc;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;

@Component
@Profile("r2dbc")
public class KnowledgePromotionReviewEntityMapper {

	private final ObjectMapper objectMapper;

	public KnowledgePromotionReviewEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public KnowledgePromotionReviewEntity toEntity(
			KnowledgePromotionReviewRecord record
	) {
		KnowledgePromotionReviewEntity entity = new KnowledgePromotionReviewEntity();
		entity.setPromotionReviewId(record.promotionReviewId());
		entity.setLearningCandidateId(record.learningCandidateId());
		entity.setIncidentId(record.incidentId());
		entity.setStatus(record.status() == null ? null : record.status().name());
		entity.setReviewedBy(record.reviewedBy());
		entity.setReviewReason(record.reviewReason());
		entity.setReviewSummary(record.reviewSummary());
		entity.setReviewedAt(record.reviewedAt());
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitize(record.metadata()),
				"Failed to serialize knowledge promotion review metadata."
		));
		return entity;
	}

	public KnowledgePromotionReviewRecord toDomain(
			KnowledgePromotionReviewEntity entity
	) {
		return new KnowledgePromotionReviewRecord(
				entity.getPromotionReviewId(),
				entity.getLearningCandidateId(),
				entity.getIncidentId(),
				entity.getStatus() == null
						? null
						: KnowledgePromotionReviewStatus.valueOf(entity.getStatus()),
				entity.getReviewedBy(),
				entity.getReviewReason(),
				entity.getReviewSummary(),
				entity.getReviewedAt(),
				sanitize(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize knowledge promotion review metadata."
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
