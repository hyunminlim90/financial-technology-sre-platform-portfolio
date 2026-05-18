package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.InMemoryExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.execution.result.InMemoryHumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;
import com.fintech.sre.agent.recommendation.verification.InMemoryVerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class GovernanceRecommendationDetailServiceTest {

	@Test
	void shouldBuildRecommendationDetailAggregate() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore = new InMemoryRecommendationApprovalStore();
		InMemoryExecutionPlanStore executionPlanStore = new InMemoryExecutionPlanStore();
		InMemoryHumanExecutionResultStore executionResultStore = new InMemoryHumanExecutionResultStore();
		InMemoryVerificationResultStore verificationStore = new InMemoryVerificationResultStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		recommendationStore.save(recommendation(base.plusSeconds(20))).block();
		approvalStore.save(approval(base.plusSeconds(10))).block();
		executionPlanStore.save(executionPlan(base.plusSeconds(30))).block();
		executionResultStore.save(executionResult(base.plusSeconds(40))).block();
		verificationStore.save(verification(base.plusSeconds(50))).block();

		GovernanceRecommendationDetailService service = service(
				recommendationStore,
				approvalStore,
				executionPlanStore,
				executionResultStore,
				verificationStore,
				resilience(),
				registry
		);

		GovernanceRecommendationDetailResponse response =
				service.findByRecommendationRecordId("rec-1").block();

		assertThat(response.recommendationRecordId()).isEqualTo("rec-1");
		assertThat(response.degradation()).isEqualTo(GovernanceDetailDegradation.none());
		assertThat(response.recommendation()).isNotNull();
		assertThat(response.approvals()).hasSize(1);
		assertThat(response.executionPlans()).hasSize(1);
		assertThat(response.humanExecutionResults()).hasSize(1);
		assertThat(response.verifications()).hasSize(1);
		assertThat(response.timeline()).isSortedAccordingTo(
				java.util.Comparator.comparing(GovernanceDetailTimelineItem::occurredAt)
		);
		assertThat(response.humanExecutionResults().get(0).summary()).isEqualTo("[redacted]");
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "recommendation")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDetailMetricName.DEGRADED_TOTAL)
				.tag("detailType", "recommendation")
				.counter()).isNull();
	}

	@Test
	void shouldReturnNotFoundWhenRecommendationRecordDoesNotExist() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceRecommendationDetailService service = service(
				new InMemoryRecommendationRecordStore(),
				new InMemoryRecommendationApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				resilience(),
				registry
		);

		assertThatThrownBy(() -> service.findByRecommendationRecordId("missing").block())
				.isInstanceOf(ResponseStatusException.class);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_NOT_FOUND)
				.tag("detailType", "recommendation")
				.tag("result", "not_found")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnPartialDegradedRecommendationDetailWhenChildComponentFails() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		InMemoryExecutionPlanStore executionPlanStore = new InMemoryExecutionPlanStore();
		InMemoryHumanExecutionResultStore executionResultStore = new InMemoryHumanExecutionResultStore();
		InMemoryVerificationResultStore verificationStore = new InMemoryVerificationResultStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		recommendationStore.save(recommendation(base.plusSeconds(20))).block();
		executionPlanStore.save(executionPlan(base.plusSeconds(30))).block();
		executionResultStore.save(executionResult(base.plusSeconds(40))).block();
		verificationStore.save(verification(base.plusSeconds(50))).block();

		GovernanceRecommendationDetailService service = service(
				recommendationStore,
				failingApprovalStore(),
				executionPlanStore,
				executionResultStore,
				verificationStore,
				resilienceEnabled(),
				registry
		);

		GovernanceRecommendationDetailResponse response =
				service.findByRecommendationRecordId("rec-1").block();

		assertThat(response.degradation().degraded()).isTrue();
		assertThat(response.degradation().failedComponents()).containsExactly("approvals");
		assertThat(response.approvals()).isEmpty();
		assertThat(response.timeline().stream()
				.map(GovernanceDetailTimelineItem::type)
				.toList()).doesNotContain("APPROVAL_DECIDED");
		assertThat(registry.find(GovernanceDetailMetricName.DEGRADED_TOTAL)
				.tag("detailType", "recommendation")
				.tag("result", "degraded")
				.tag("reason", "component_query_failed")
				.tag("component", "approvals")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldPropagateRecommendationChildFailureWhenPartialResponseDisabled() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		recommendationStore.save(recommendation(base.plusSeconds(20))).block();

		GovernanceRecommendationDetailService service = service(
				recommendationStore,
				failingApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				partialDisabled(),
				new SimpleMeterRegistry()
		);

		assertThatThrownBy(() -> service.findByRecommendationRecordId("rec-1").block())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldPropagateRecommendationChildFailureWhenFailOpenDisabled() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		recommendationStore.save(recommendation(base.plusSeconds(20))).block();

		GovernanceRecommendationDetailService service = service(
				recommendationStore,
				failingApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				failOpenDisabled(),
				new SimpleMeterRegistry()
		);

		assertThatThrownBy(() -> service.findByRecommendationRecordId("rec-1").block())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldRecordFailureMetricWhenRecommendationDetailQueryFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceRecommendationDetailService service = service(
				failingRecommendationRecordStore(),
				new InMemoryRecommendationApprovalStore(),
				new InMemoryExecutionPlanStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				resilience(),
				registry
		);

		assertThatThrownBy(() -> service.findByRecommendationRecordId("rec-1").block())
				.isInstanceOf(IllegalStateException.class);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "recommendation")
				.tag("result", "failure")
				.counter()
				.count()).isEqualTo(1.0);
	}

	private GovernanceRecommendationDetailService service(
			RecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			ExecutionPlanStore executionPlanStore,
			HumanExecutionResultStore executionResultStore,
			VerificationResultStore verificationStore,
			GovernanceDetailResilienceProperties properties,
			SimpleMeterRegistry registry
	) {
		return new GovernanceRecommendationDetailService(
				recommendationStore,
				approvalStore,
				executionPlanStore,
				executionResultStore,
				verificationStore,
				new GovernanceDetailTimelineBuilder(new GovernanceDetailSanitizer()),
				new GovernanceDetailSanitizer(),
				new GovernanceDetailComponentLoader(properties),
				recorder(registry)
		);
	}

	private RecommendationRecordStore failingRecommendationRecordStore() {
		return new RecommendationRecordStore() {
			@Override
			public Mono<RecommendationRecord> save(RecommendationRecord record) {
				return Mono.just(record);
			}

			@Override
			public Mono<RecommendationRecord> findById(String recommendationRecordId) {
				return Mono.error(new IllegalStateException("boom"));
			}

			@Override
			public Flux<RecommendationRecord> findByIncidentId(String incidentId) {
				return Flux.empty();
			}

			@Override
			public Flux<RecommendationRecord> findRecent(int limit) {
				return Flux.empty();
			}
		};
	}

	private RecommendationApprovalStore failingApprovalStore() {
		return new RecommendationApprovalStore() {
			@Override
			public Mono<RecommendationApprovalRecord> save(RecommendationApprovalRecord record) {
				return Mono.just(record);
			}

			@Override
			public Flux<RecommendationApprovalRecord> findByIncidentId(String incidentId) {
				return Flux.empty();
			}

			@Override
			public Flux<RecommendationApprovalRecord> findByRecommendationRecordId(String recommendationRecordId) {
				return Flux.error(new IllegalStateException("boom"));
			}

			@Override
			public Mono<RecommendationApprovalRecord> findLatestByRecommendationRecordId(String recommendationRecordId) {
				return Mono.empty();
			}

			@Override
			public Flux<RecommendationApprovalRecord> findRecent(int limit) {
				return Flux.empty();
			}
		};
	}

	private GovernanceDetailMetricsRecorder recorder(SimpleMeterRegistry registry) {
		return new GovernanceDetailMetricsRecorder(
				new GovernanceMetricsRecorder(registry)
		);
	}

	private GovernanceDetailResilienceProperties resilience() {
		return new GovernanceDetailResilienceProperties();
	}

	private GovernanceDetailResilienceProperties resilienceEnabled() {
		GovernanceDetailResilienceProperties properties = new GovernanceDetailResilienceProperties();
		properties.setEnabled(true);
		return properties;
	}

	private GovernanceDetailResilienceProperties partialDisabled() {
		GovernanceDetailResilienceProperties properties = resilienceEnabled();
		properties.setPartialResponseEnabled(false);
		return properties;
	}

	private GovernanceDetailResilienceProperties failOpenDisabled() {
		GovernanceDetailResilienceProperties properties = resilienceEnabled();
		properties.setFailOpenDetail(false);
		return properties;
	}

	private RecommendationRecord recommendation(Instant occurredAt) {
		return new RecommendationRecord(
				"rec-1",
				"incident-1",
				"audit-1",
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				occurredAt,
				1,
				0,
				"ALLOW",
				"PASS",
				List.of("RATE_LIMIT"),
				List.of(),
				Map.of()
		);
	}

	private RecommendationApprovalRecord approval(Instant occurredAt) {
		return new RecommendationApprovalRecord(
				"approval-1",
				"rec-1",
				"incident-1",
				RecommendationApprovalStatus.APPROVED,
				"operator-a",
				"approved",
				occurredAt,
				Map.of()
		);
	}

	private RecommendationExecutionPlan executionPlan(Instant occurredAt) {
		return new RecommendationExecutionPlan(
				"plan-1",
				"rec-1",
				"incident-1",
				ExecutionPlanStatus.DRY_RUN_PLAN_CREATED,
				false,
				true,
				"operator-a",
				"plan",
				occurredAt,
				List.of(),
				List.of()
		);
	}

	private HumanExecutionResultRecord executionResult(Instant occurredAt) {
		return new HumanExecutionResultRecord(
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				HumanExecutionStatus.EXECUTED,
				"operator-a",
				"rawLog token should not appear",
				occurredAt.minusSeconds(10),
				occurredAt.minusSeconds(5),
				occurredAt,
				Map.of()
		);
	}

	private VerificationResultRecord verification(Instant occurredAt) {
		return new VerificationResultRecord(
				"verification-1",
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				VerificationStatus.VERIFIED,
				"operator-a",
				"verified",
				occurredAt,
				Map.of()
		);
	}
}
