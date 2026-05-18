package com.fintech.sre.agent.recommendation.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalDecision;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

class RecommendationExecutionPlanServiceTest {

	@Test
	void shouldCreateDryRunPlanForApprovedRecommendation() {
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();

		recommendationStore.save(recommendation()).block();
		approvalStore.save(approval(RecommendationApprovalStatus.APPROVED)).block();

		RecommendationExecutionPlanService service = new RecommendationExecutionPlanService(
				recommendationStore,
				approvalStore,
				new InMemoryExecutionPlanStore(),
				new ExecutionPlanStepMapper(),
				new ExecutionPlanIdGenerator(),
				MetricsRecorderTestSupport.executionMetricsRecorder()
		);

		ExecutionPlanResponse response = service.createDryRunPlan(
				"rec-1",
				new ExecutionPlanRequest(
						"operator-a",
						"Prepare dry-run execution plan for human review."
				)
		).block();

		assertThat(response.executable()).isFalse();
		assertThat(response.status()).isEqualTo(ExecutionPlanStatus.DRY_RUN_PLAN_CREATED);
		assertThat(response.requiresFinalApproval()).isTrue();
		assertThat(response.steps()).hasSize(1);
		assertThat(response.steps().get(0).rollbackRequired()).isTrue();
		assertThat(response.steps().get(0).verificationRequired()).isTrue();
	}

	@Test
	void shouldRejectWhenRecommendationIsNotApproved() {
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();

		recommendationStore.save(recommendation()).block();
		approvalStore.save(approval(RecommendationApprovalStatus.REJECTED)).block();

		RecommendationExecutionPlanService service = new RecommendationExecutionPlanService(
				recommendationStore,
				approvalStore,
				new InMemoryExecutionPlanStore(),
				new ExecutionPlanStepMapper(),
				new ExecutionPlanIdGenerator(),
				MetricsRecorderTestSupport.executionMetricsRecorder()
		);

		assertThatThrownBy(() -> service.createDryRunPlan(
				"rec-1",
				new ExecutionPlanRequest("operator-a", "reason")
		).block())
				.isInstanceOf(ExecutionPlanRejectedException.class)
				.hasMessage("Only APPROVED recommendation can create execution plan.");
	}

	@Test
	void shouldRejectWhenApprovalMissing() {
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();
		recommendationStore.save(recommendation()).block();

		RecommendationExecutionPlanService service = new RecommendationExecutionPlanService(
				recommendationStore,
				new InMemoryRecommendationApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new ExecutionPlanStepMapper(),
				new ExecutionPlanIdGenerator(),
				MetricsRecorderTestSupport.executionMetricsRecorder()
		);

		assertThatThrownBy(() -> service.createDryRunPlan(
				"rec-1",
				new ExecutionPlanRequest("operator-a", "reason")
		).block())
				.isInstanceOf(ExecutionPlanRejectedException.class)
				.hasMessage("Recommendation must be approved before creating execution plan.");
	}

	private RecommendationRecord recommendation() {
		return new RecommendationRecord(
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
				Map.of("alertName", "HighP99Latency")
		);
	}

	private RecommendationApprovalRecord approval(RecommendationApprovalStatus status) {
		return new RecommendationApprovalRecord(
				"approval-1",
				"rec-1",
				"incident-1",
				status,
				"operator-a",
				status == RecommendationApprovalStatus.APPROVED
						? "approved"
						: RecommendationApprovalDecision.REJECTED.name().toLowerCase(),
				Instant.now(),
				Map.of()
		);
	}
}
