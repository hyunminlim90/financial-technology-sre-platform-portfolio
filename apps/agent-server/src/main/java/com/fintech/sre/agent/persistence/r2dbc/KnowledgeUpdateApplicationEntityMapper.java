package com.fintech.sre.agent.persistence.r2dbc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;

@Component
@Profile("r2dbc")
public class KnowledgeUpdateApplicationEntityMapper {

	private final ObjectMapper objectMapper;

	public KnowledgeUpdateApplicationEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public KnowledgeUpdateApplicationEntity toEntity(
			KnowledgeUpdateApplicationRecord record
	) {
		KnowledgeUpdateApplicationEntity entity = new KnowledgeUpdateApplicationEntity();
		entity.setKnowledgeUpdateApplicationId(record.knowledgeUpdateApplicationId());
		entity.setIncidentId(record.incidentId());
		entity.setLearningCandidateId(record.learningCandidateId());
		entity.setPromotionPlanId(record.promotionPlanId());
		entity.setKnowledgeType(record.knowledgeType());
		entity.setKnowledgeLayer(record.knowledgeLayer() == null
				? null
				: record.knowledgeLayer().name());
		entity.setFilePath(record.filePath());
		entity.setChangeType(record.changeType() == null ? null : record.changeType().name());
		entity.setGitRepository(record.gitRepository());
		entity.setGitBranch(record.gitBranch());
		entity.setGitCommitSha(record.gitCommitSha());
		entity.setPullRequestReference(record.pullRequestReference());
		entity.setAppliedBy(record.appliedBy());
		entity.setReviewedBy(record.reviewedBy());
		entity.setApprovedBy(record.approvedBy());
		entity.setValidationChecksJson(JsonUtils.toJsonArray(
				objectMapper,
				sanitizeValidationChecks(record.validationChecks()),
				"Failed to serialize knowledge update validation checks."
		));
		entity.setAppliedAt(record.appliedAt());
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitizeMetadata(record.metadata()),
				"Failed to serialize knowledge update metadata."
		));
		return entity;
	}

	public KnowledgeUpdateApplicationRecord toDomain(
			KnowledgeUpdateApplicationEntity entity
	) {
		return new KnowledgeUpdateApplicationRecord(
				entity.getKnowledgeUpdateApplicationId(),
				entity.getIncidentId(),
				entity.getLearningCandidateId(),
				entity.getPromotionPlanId(),
				entity.getKnowledgeType(),
				entity.getKnowledgeLayer() == null
						? null
						: KnowledgeUpdateLayer.valueOf(entity.getKnowledgeLayer()),
				entity.getFilePath(),
				entity.getChangeType() == null
						? null
						: KnowledgeUpdateChangeType.valueOf(entity.getChangeType()),
				entity.getGitRepository(),
				entity.getGitBranch(),
				entity.getGitCommitSha(),
				entity.getPullRequestReference(),
				entity.getAppliedBy(),
				entity.getReviewedBy(),
				entity.getApprovedBy(),
				sanitizeValidationChecks(JsonUtils.toStringList(
						objectMapper,
						entity.getValidationChecksJson(),
						"Failed to deserialize knowledge update validation checks."
				)),
				entity.getAppliedAt(),
				sanitizeMetadata(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize knowledge update metadata."
				))
		);
	}

	private List<String> sanitizeValidationChecks(List<String> checks) {
		if (checks == null || checks.isEmpty()) {
			return List.of();
		}

		return checks.stream()
				.filter(this::allowedValidationCheck)
				.toList();
	}

	private boolean allowedValidationCheck(String check) {
		if (check == null) {
			return false;
		}

		String lower = check.toLowerCase();
		return !lower.contains("customer")
				&& !lower.contains("secret")
				&& !lower.contains("token")
				&& !lower.contains("payment payload")
				&& !lower.contains("raw log");
	}

	private Map<String, String> sanitizeMetadata(Map<String, String> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return Map.of();
		}

		return metadata.entrySet().stream()
				.filter(entry -> allowedMetadataKey(entry.getKey()))
				.collect(Collectors.toUnmodifiableMap(
						Map.Entry::getKey,
						Map.Entry::getValue
				));
	}

	private boolean allowedMetadataKey(String key) {
		if (key == null) {
			return false;
		}

		String lower = key.toLowerCase();
		return !lower.contains("payload")
				&& !lower.contains("customer")
				&& !lower.contains("secret")
				&& !lower.contains("token")
				&& !lower.contains("payment")
				&& !lower.contains("prompt")
				&& !lower.contains("rawlog")
				&& !lower.contains("password");
	}
}
