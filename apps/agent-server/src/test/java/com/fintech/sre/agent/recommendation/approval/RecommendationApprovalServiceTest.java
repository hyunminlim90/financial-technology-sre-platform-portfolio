package com.fintech.sre.agent.recommendation.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;
import com.fintech.sre.agent.recommendation.approval.audit.InMemoryRecommendationApprovalAuditLogger;
import com.fintech.sre.agent.recommendation.approval.audit.RecommendationApprovalAuditLog;
import com.fintech.sre.agent.recommendation.approval.audit.RecommendationApprovalAuditLogger;
import com.fintech.sre.agent.recommendation.approval.audit.RecommendationApprovalAuditMapper;
import com.fintech.sre.agent.recommendation.approval.audit.RecommendationApprovalAuditIdGenerator;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class RecommendationApprovalServiceTest {

	@Test
	void shouldApproveRecommendationWithoutExecutingAction() {
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();

		RecommendationRecord recommendation = new RecommendationRecord(
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

		recommendationStore.save(recommendation).block();

		InMemoryRecommendationApprovalStore approvalStore =
				new InMemoryRecommendationApprovalStore();
		InMemoryRecommendationApprovalAuditLogger auditLogger =
				new InMemoryRecommendationApprovalAuditLogger();

		RecommendationApprovalService service = new RecommendationApprovalService(
				recommendationStore,
				approvalStore,
				new RecommendationApprovalIdGenerator(),
				auditLogger,
				new RecommendationApprovalAuditMapper(
						new RecommendationApprovalAuditIdGenerator()
				),
				MetricsRecorderTestSupport.approvalMetricsRecorder()
		);

		RecommendationApprovalResponse response = service.decide(
				"rec-1",
				new RecommendationApprovalRequest(
						RecommendationApprovalDecision.APPROVED,
						"operator-a",
						"Runbook and evidence match current incident.",
						Map.of()
				)
		).block();

		assertThat(response.status()).isEqualTo(RecommendationApprovalStatus.APPROVED);
		assertThat(approvalStore.findLatestByRecommendationRecordId("rec-1").block().status())
				.isEqualTo(RecommendationApprovalStatus.APPROVED);
		assertThat(auditLogger.findByRecommendationRecordId("rec-1").collectList().block())
				.hasSize(1);
	}

	@Test
	void shouldRejectWhenRecommendationNotFound() {
		RecommendationApprovalService service = new RecommendationApprovalService(
				new InMemoryRecommendationRecordStore(),
				new InMemoryRecommendationApprovalStore(),
				new RecommendationApprovalIdGenerator(),
				new InMemoryRecommendationApprovalAuditLogger(),
				new RecommendationApprovalAuditMapper(
						new RecommendationApprovalAuditIdGenerator()
				),
				MetricsRecorderTestSupport.approvalMetricsRecorder()
		);

		assertThatThrownBy(() -> service.decide(
				"missing",
				new RecommendationApprovalRequest(
						RecommendationApprovalDecision.APPROVED,
						"operator-a",
						"reason",
						Map.of()
				)
		).block())
				.isInstanceOf(RecommendationApprovalRejectedException.class)
				.hasMessage("Recommendation record not found.");
	}

	@Test
	void shouldIgnoreAuditFailureWhenApprovalSucceeds() {
		InMemoryRecommendationRecordStore recommendationStore =
				new InMemoryRecommendationRecordStore();

		recommendationStore.save(new RecommendationRecord(
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
		)).block();

		RecommendationApprovalService service = new RecommendationApprovalService(
				recommendationStore,
				new InMemoryRecommendationApprovalStore(),
				new RecommendationApprovalIdGenerator(),
				new FailingAuditLogger(),
				new RecommendationApprovalAuditMapper(
						new RecommendationApprovalAuditIdGenerator()
				),
				MetricsRecorderTestSupport.approvalMetricsRecorder()
		);

		RecommendationApprovalResponse response = service.decide(
				"rec-1",
				new RecommendationApprovalRequest(
						RecommendationApprovalDecision.APPROVED,
						"operator-a",
						"reason",
						Map.of("team", "sre")
				)
		).block();

		assertThat(response.status()).isEqualTo(RecommendationApprovalStatus.APPROVED);
	}

	private static final class FailingAuditLogger implements RecommendationApprovalAuditLogger {

		@Override
		public Mono<Void> log(RecommendationApprovalAuditLog log) {
			return Mono.error(new IllegalStateException("audit unavailable"));
		}

		@Override
		public Flux<RecommendationApprovalAuditLog> findByIncidentId(String incidentId) {
			return Flux.empty();
		}

		@Override
		public Flux<RecommendationApprovalAuditLog> findByRecommendationRecordId(
				String recommendationRecordId
		) {
			return Flux.empty();
		}
	}
}
