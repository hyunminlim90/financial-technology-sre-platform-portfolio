package com.fintech.sre.agent.persistence.r2dbc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanTarget;

@Component
@Profile("r2dbc")
public class KnowledgePromotionPlanEntityMapper {

	private static final TypeReference<List<KnowledgePromotionPlanTarget>> TARGET_LIST =
			new TypeReference<>() { };

	private final ObjectMapper objectMapper;

	public KnowledgePromotionPlanEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public KnowledgePromotionPlanEntity toEntity(
			KnowledgePromotionPlanRecord record
	) {
		KnowledgePromotionPlanEntity entity = new KnowledgePromotionPlanEntity();
		entity.setPromotionPlanId(record.promotionPlanId());
		entity.setLearningCandidateId(record.learningCandidateId());
		entity.setIncidentId(record.incidentId());
		entity.setStatus(record.status() == null ? null : record.status().name());
		entity.setPlannedBy(record.plannedBy());
		entity.setSummary(record.summary());
		entity.setTargetsJson(JsonUtils.toJsonValue(
				objectMapper,
				record.targets() == null ? List.of() : record.targets(),
				"Failed to serialize knowledge promotion plan targets."
		));
		entity.setRequiredHumanChecksJson(JsonUtils.toJsonArray(
				objectMapper,
				record.requiredHumanChecks(),
				"Failed to serialize knowledge promotion required human checks."
		));
		entity.setBlockedReasonsJson(JsonUtils.toJsonArray(
				objectMapper,
				record.blockedReasons(),
				"Failed to serialize knowledge promotion blocked reasons."
		));
		entity.setCreatedAt(record.createdAt());
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				sanitize(record.metadata()),
				"Failed to serialize knowledge promotion plan metadata."
		));
		return entity;
	}

	public KnowledgePromotionPlanRecord toDomain(
			KnowledgePromotionPlanEntity entity
	) {
		List<KnowledgePromotionPlanTarget> targets = JsonUtils.toValue(
				objectMapper,
				entity.getTargetsJson(),
				TARGET_LIST,
				"Failed to deserialize knowledge promotion plan targets."
		);

		return new KnowledgePromotionPlanRecord(
				entity.getPromotionPlanId(),
				entity.getLearningCandidateId(),
				entity.getIncidentId(),
				entity.getStatus() == null
						? null
						: KnowledgePromotionPlanStatus.valueOf(entity.getStatus()),
				entity.getPlannedBy(),
				entity.getSummary(),
				targets == null ? List.of() : List.copyOf(targets),
				JsonUtils.toStringList(
						objectMapper,
						entity.getRequiredHumanChecksJson(),
						"Failed to deserialize knowledge promotion required human checks."
				),
				JsonUtils.toStringList(
						objectMapper,
						entity.getBlockedReasonsJson(),
						"Failed to deserialize knowledge promotion blocked reasons."
				),
				entity.getCreatedAt(),
				sanitize(JsonUtils.toStringMap(
						objectMapper,
						entity.getMetadataJson(),
						"Failed to deserialize knowledge promotion plan metadata."
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
				&& !lower.contains("prompt")
				&& !lower.contains("rawlog");
	}
}
