package com.fintech.sre.agent.persistence.r2dbc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;

@Component
@Profile("r2dbc")
public class LearningCandidateEntityMapper {

	private final ObjectMapper objectMapper;

	public LearningCandidateEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public LearningCandidateEntity toEntity(LearningCandidateRecord record) {
		LearningCandidateEntity entity = new LearningCandidateEntity();
		entity.setLearningCandidateId(record.learningCandidateId());
		entity.setIncidentId(record.incidentId());
		entity.setPostmortemDraftId(record.postmortemDraftId());
		entity.setPostmortemReviewId(record.postmortemReviewId());
		entity.setType(record.type() == null ? null : record.type().name());
		entity.setStatus(record.status() == null ? null : record.status().name());
		entity.setPromotedBy(record.promotedBy());
		entity.setSummary(record.summary());
		entity.setProposedChangesJson(JsonUtils.toJsonArray(
				objectMapper,
				sanitizeProposedChanges(record.proposedChanges()),
				"Failed to serialize learning candidate proposed changes."
		));
		entity.setCreatedAt(record.createdAt());
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitizeMetadata(record.metadata()),
				"Failed to serialize learning candidate metadata."
		));
		return entity;
	}

	public LearningCandidateRecord toDomain(LearningCandidateEntity entity) {
		return new LearningCandidateRecord(
				entity.getLearningCandidateId(),
				entity.getIncidentId(),
				entity.getPostmortemDraftId(),
				entity.getPostmortemReviewId(),
				entity.getType() == null ? null : LearningCandidateType.valueOf(entity.getType()),
				entity.getStatus() == null ? null : LearningCandidateStatus.valueOf(entity.getStatus()),
				entity.getPromotedBy(),
				entity.getSummary(),
				sanitizeProposedChanges(JsonUtils.toStringList(
						objectMapper,
						entity.getProposedChangesJson(),
						"Failed to deserialize learning candidate proposed changes."
				)),
				entity.getCreatedAt(),
				sanitizeMetadata(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize learning candidate metadata."
				))
		);
	}

	private List<String> sanitizeProposedChanges(List<String> changes) {
		if (changes == null || changes.isEmpty()) {
			return List.of();
		}

		return changes.stream()
				.filter(change -> allowedChange(change))
				.toList();
	}

	private boolean allowedChange(String change) {
		if (change == null) {
			return false;
		}

		String lower = change.toLowerCase();
		return !lower.contains("payment payload")
				&& !lower.contains("customer")
				&& !lower.contains("secret")
				&& !lower.contains("token")
				&& !lower.contains("raw log")
				&& !lower.contains("full prompt");
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
				&& !lower.contains("rawlog");
	}
}
