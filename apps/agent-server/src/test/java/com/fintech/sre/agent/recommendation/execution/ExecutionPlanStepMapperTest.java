package com.fintech.sre.agent.recommendation.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

class ExecutionPlanStepMapperTest {

	private final ExecutionPlanStepMapper mapper = new ExecutionPlanStepMapper();

	@Test
	void shouldMapActionTypesToDryRunSteps() {
		RecommendationRecord record = new RecommendationRecord(
				"rec-1",
				"incident-1",
				"audit-1",
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				Instant.now(),
				1,
				0,
				"ALLOW",
				"PASS",
				List.of("RATE_LIMIT"),
				List.of(),
				Map.of()
		);

		List<ExecutionPlanStep> steps = mapper.toSteps(record);

		assertThat(steps).hasSize(1);
		assertThat(steps.get(0).actionType()).isEqualTo("RATE_LIMIT");
		assertThat(steps.get(0).dryRunOnly()).isTrue();
	}
}
