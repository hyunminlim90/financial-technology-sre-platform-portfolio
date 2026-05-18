package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStep;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;

class RecommendationExecutionPlanEntityMapperTest {

	@Test
	void shouldRoundTripExecutionPlanAndPreserveDryRunFields() {
		RecommendationExecutionPlanEntityMapper mapper =
				new RecommendationExecutionPlanEntityMapper(new ObjectMapper());

		RecommendationExecutionPlan plan = new RecommendationExecutionPlan(
				"plan-1",
				"rec-1",
				"incident-1",
				ExecutionPlanStatus.DRY_RUN_PLAN_CREATED,
				false,
				true,
				"operator-a",
				"dry-run",
				Instant.parse("2026-05-09T00:00:00Z"),
				List.of(new ExecutionPlanStep(
						"RATE_LIMIT",
						"payment-api",
						"application",
						"HIGH",
						"SINGLE_SERVICE",
						true,
						true,
						true,
						true,
						true,
						Map.of("threshold", "100")
				)),
				List.of("NO_SCENARIO")
		);

		RecommendationExecutionPlanEntity entity = mapper.toEntity(plan);
		RecommendationExecutionPlan restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).isEqualTo("{}");
		assertThat(restored.executable()).isFalse();
		assertThat(restored.requiresFinalApproval()).isTrue();
		assertThat(restored.steps()).hasSize(1);
		assertThat(restored.steps().get(0).actionType()).isEqualTo("RATE_LIMIT");
		assertThat(restored.blockedReasons()).containsExactly("NO_SCENARIO");
	}
}
