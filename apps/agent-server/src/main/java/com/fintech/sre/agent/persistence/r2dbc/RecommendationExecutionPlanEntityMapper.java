package com.fintech.sre.agent.persistence.r2dbc;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStep;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;

@Component
@Profile("r2dbc")
public class RecommendationExecutionPlanEntityMapper {

	private final ObjectMapper objectMapper;

	public RecommendationExecutionPlanEntityMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public RecommendationExecutionPlanEntity toEntity(
			RecommendationExecutionPlan plan
	) {
		RecommendationExecutionPlanEntity entity =
				new RecommendationExecutionPlanEntity();
		entity.setExecutionPlanId(plan.executionPlanId());
		entity.setRecommendationRecordId(plan.recommendationRecordId());
		entity.setIncidentId(plan.incidentId());
		entity.setStatus(
				plan.status() == null ? null : plan.status().name()
		);
		entity.setExecutable(plan.executable());
		entity.setRequiresFinalApproval(plan.requiresFinalApproval());
		entity.setCreatedBy(plan.createdBy());
		entity.setReason(plan.reason());
		entity.setCreatedAt(plan.createdAt());
		entity.setStepsJson(JsonUtils.toJsonValue(
				objectMapper,
				plan.steps(),
				"Failed to serialize execution plan steps."
		));
		entity.setBlockedReasonsJson(JsonUtils.toJsonArray(
				objectMapper,
				plan.blockedReasons(),
				"Failed to serialize execution plan blocked reasons."
		));
		entity.setMetadataJson(JsonUtils.toJsonObject(
				objectMapper,
				Map.of(),
				"Failed to serialize execution plan metadata."
		));
		return entity;
	}

	public RecommendationExecutionPlan toDomain(
			RecommendationExecutionPlanEntity entity
	) {
		return new RecommendationExecutionPlan(
				entity.getExecutionPlanId(),
				entity.getRecommendationRecordId(),
				entity.getIncidentId(),
				entity.getStatus() == null
						? null
						: ExecutionPlanStatus.valueOf(entity.getStatus()),
				entity.isExecutable(),
				entity.isRequiresFinalApproval(),
				entity.getCreatedBy(),
				entity.getReason(),
				entity.getCreatedAt(),
				JsonUtils.toValue(
						objectMapper,
						entity.getStepsJson(),
						JsonUtils.executionPlanStepListType(),
						"Failed to deserialize execution plan steps."
				),
				JsonUtils.toStringList(
						objectMapper,
						entity.getBlockedReasonsJson(),
						"Failed to deserialize execution plan blocked reasons."
				)
		);
	}
}
