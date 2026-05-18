package com.fintech.sre.agent.recommendation.execution.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.InMemoryExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;

class HumanExecutionResultServiceTest {

	@Test
	void shouldRecordHumanExecutionResultForDryRunPlan() {
		InMemoryExecutionPlanStore executionPlanStore = new InMemoryExecutionPlanStore();
		InMemoryHumanExecutionResultStore resultStore = new InMemoryHumanExecutionResultStore();

		executionPlanStore.save(plan("plan-1", ExecutionPlanStatus.DRY_RUN_PLAN_CREATED)).block();

		HumanExecutionResultService service = new HumanExecutionResultService(
				executionPlanStore,
				resultStore,
				new HumanExecutionResultIdGenerator(),
				MetricsRecorderTestSupport.executionMetricsRecorder()
		);

		HumanExecutionResultResponse response = service.record(
				"plan-1",
				new HumanExecutionResultRequest(
						HumanExecutionStatus.EXECUTED,
						"operator-a",
						"manual action applied",
						Instant.now().minusSeconds(60),
						Instant.now(),
						Map.of(
								"ticket", "INC-123",
								"secret", "must-not-store",
								"rawLog", "must-not-store"
						)
				)
		).block();

		assertThat(response.status()).isEqualTo(HumanExecutionStatus.EXECUTED);

		HumanExecutionResultRecord record = resultStore.findById(response.executionResultId()).block();
		assertThat(record.metadata()).containsKey("ticket");
		assertThat(record.metadata()).doesNotContainKey("secret");
		assertThat(record.metadata()).doesNotContainKey("rawLog");
	}

	@Test
	void shouldRejectWhenPlanIsBlocked() {
		InMemoryExecutionPlanStore executionPlanStore = new InMemoryExecutionPlanStore();
		executionPlanStore.save(plan("plan-1", ExecutionPlanStatus.BLOCKED)).block();

		HumanExecutionResultService service = new HumanExecutionResultService(
				executionPlanStore,
				new InMemoryHumanExecutionResultStore(),
				new HumanExecutionResultIdGenerator(),
				MetricsRecorderTestSupport.executionMetricsRecorder()
		);

		assertThatThrownBy(() -> service.record(
				"plan-1",
				new HumanExecutionResultRequest(
						HumanExecutionStatus.FAILED,
						"operator-a",
						"could not run",
						Instant.now().minusSeconds(60),
						Instant.now(),
						Map.of()
				)
		).block())
				.isInstanceOf(HumanExecutionResultRejectedException.class)
				.hasMessage("Only DRY_RUN_PLAN_CREATED plans can accept human execution result.");
	}

	private RecommendationExecutionPlan plan(String id, ExecutionPlanStatus status) {
		return new RecommendationExecutionPlan(
				id,
				"rec-1",
				"incident-1",
				status,
				false,
				true,
				"operator-a",
				"dry-run",
				Instant.now(),
				List.of(),
				List.of()
		);
	}
}
