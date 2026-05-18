package com.fintech.sre.agent.persistence.r2dbc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;

@Component
@Profile("r2dbc")
public class PostmortemDraftEntityMapper {

	private final ObjectMapper objectMapper;

	public PostmortemDraftEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public PostmortemDraftEntity toEntity(PostmortemDraftRecord record) {
		PostmortemDraftEntity entity = new PostmortemDraftEntity();
		entity.setPostmortemDraftId(record.postmortemDraftId());
		entity.setIncidentId(record.incidentId());
		entity.setStatus(record.status() == null ? null : record.status().name());
		entity.setRequestedBy(record.requestedBy());
		entity.setSummary(record.summary());
		entity.setTimelineJson(toJsonArray(
				record.timeline(),
				"Failed to serialize postmortem draft timeline."
		));
		entity.setRecommendationsJson(toJsonArray(
				record.recommendations(),
				"Failed to serialize postmortem draft recommendations."
		));
		entity.setExecutionResultsJson(toJsonArray(
				record.executionResults(),
				"Failed to serialize postmortem draft execution results."
		));
		entity.setVerificationResultsJson(toJsonArray(
				record.verificationResults(),
				"Failed to serialize postmortem draft verification results."
		));
		entity.setReanalysisCandidatesJson(toJsonArray(
				record.reanalysisCandidates(),
				"Failed to serialize postmortem draft reanalysis candidates."
		));
		entity.setLearningCandidatesJson(toJsonArray(
				record.learningCandidates(),
				"Failed to serialize postmortem draft learning candidates."
		));
		entity.setOpenQuestionsJson(toJsonArray(
				record.openQuestions(),
				"Failed to serialize postmortem draft open questions."
		));
		entity.setCreatedAt(record.createdAt());
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitize(record.metadata()),
				"Failed to serialize postmortem draft metadata."
		));
		return entity;
	}

	public PostmortemDraftRecord toDomain(PostmortemDraftEntity entity) {
		return new PostmortemDraftRecord(
				entity.getPostmortemDraftId(),
				entity.getIncidentId(),
				entity.getStatus() == null
						? null
						: PostmortemDraftStatus.valueOf(entity.getStatus()),
				entity.getRequestedBy(),
				entity.getSummary(),
				toStringList(
						entity.getTimelineJson(),
						"Failed to deserialize postmortem draft timeline."
				),
				toStringList(
						entity.getRecommendationsJson(),
						"Failed to deserialize postmortem draft recommendations."
				),
				toStringList(
						entity.getExecutionResultsJson(),
						"Failed to deserialize postmortem draft execution results."
				),
				toStringList(
						entity.getVerificationResultsJson(),
						"Failed to deserialize postmortem draft verification results."
				),
				toStringList(
						entity.getReanalysisCandidatesJson(),
						"Failed to deserialize postmortem draft reanalysis candidates."
				),
				toStringList(
						entity.getLearningCandidatesJson(),
						"Failed to deserialize postmortem draft learning candidates."
				),
				toStringList(
						entity.getOpenQuestionsJson(),
						"Failed to deserialize postmortem draft open questions."
				),
				entity.getCreatedAt(),
				sanitize(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize postmortem draft metadata."
				))
		);
	}

	private String toJsonArray(List<String> value, String errorMessage) {
		return JsonUtils.toJsonArray(objectMapper, value, errorMessage);
	}

	private List<String> toStringList(String value, String errorMessage) {
		return JsonUtils.toStringList(objectMapper, value, errorMessage);
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
