package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceCollectionOrchestratorTest {

	private final EvidenceCollectionOrchestrator orchestrator =
			new EvidenceCollectionOrchestrator();

	@Test
	void shouldRemainSemanticCollectorRatherThanObservabilityEngine() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(adapter(collected(
						EvidenceSourceType.METRICS,
						signal(EvidenceSignalType.METRIC, "metric-1", "summary-1")
				))),
				List.of(query(EvidenceSourceType.METRICS, false))
		));

		assertThat(result.exposesRawObservabilityPayload()).isFalse();
		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
	}

	@Test
	void shouldMergeOnlyNormalizedEvidenceSignals() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(
						adapter(collected(
								EvidenceSourceType.METRICS,
								signal(EvidenceSignalType.METRIC, "metric-1", "metric")
						)),
						adapter(collected(
								EvidenceSourceType.LOGS,
								signal(EvidenceSignalType.LOG, "log-1", "log")
						))
				),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.LOGS, false)
				)
		));

		assertThat(result.normalizedSignals()).extracting(EvidenceSignal::type)
				.containsExactly(EvidenceSignalType.METRIC, EvidenceSignalType.LOG);
	}

	@Test
	void shouldTreatPartialAdapterFailureAsPartialRatherThanSystemFailure() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(
						adapter(collected(
								EvidenceSourceType.METRICS,
								signal(EvidenceSignalType.METRIC, "metric-1", "metric")
						)),
						adapter(failed(EvidenceSourceType.LOGS))
				),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.LOGS, false)
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceCollectionStatus.PARTIAL);
		assertThat(result.uncertainty()).isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldTreatUnknownOnlyCollectionAsUnknown() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(
						adapter(unknown(EvidenceSourceType.METRICS)),
						adapter(unknown(EvidenceSourceType.LOGS))
				),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.LOGS, false)
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceCollectionStatus.UNKNOWN);
	}

	@Test
	void shouldTreatAllAdapterFailuresAsFailed() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(
						adapter(failed(EvidenceSourceType.METRICS)),
						adapter(failed(EvidenceSourceType.LOGS))
				),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.LOGS, false)
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceCollectionStatus.FAILED);
	}

	@Test
	void shouldKeepPaymentSafetyUncertaintyWhenPaymentConsistencySourceMissing() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(adapter(collected(
						EvidenceSourceType.METRICS,
						signal(EvidenceSignalType.METRIC, "metric-1", "metric")
				))),
				List.of(query(EvidenceSourceType.METRICS, false))
		));

		assertThat(result.paymentSafetyUncertain()).isTrue();
		assertThat(result.uncertainty()).isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldClearPaymentSafetyUncertaintyWhenPaymentConsistencySourcePresent() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(
						adapter(collected(
								EvidenceSourceType.METRICS,
								signal(EvidenceSignalType.METRIC, "metric-1", "metric")
						)),
						adapter(new EvidenceQueryResult(
								EvidenceSourceType.PAYMENT_CONSISTENCY,
								EvidenceCollectionStatus.COLLECTED,
								List.of(signal(
										EvidenceSignalType.PAYMENT_SAFETY,
										"payment-1",
										"payment"
								)),
								true
						))
				),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.PAYMENT_CONSISTENCY, true)
				)
		));

		assertThat(result.paymentSafetyUncertain()).isFalse();
	}

	@Test
	void shouldPreserveContradictionMarkerForContradictorySignals() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(
						adapter(collected(
								EvidenceSourceType.LOGS,
								signal(EvidenceSignalType.LOG, "shared-1", "healthy")
						)),
						adapter(collected(
								EvidenceSourceType.LOGS,
								signal(EvidenceSignalType.LOG, "shared-1", "degraded")
						))
				),
				List.of(
						query(EvidenceSourceType.LOGS, false),
						query(EvidenceSourceType.LOGS, false)
				)
		));

		assertThat(result.contradictionMarkerPresent()).isTrue();
		assertThat(result.uncertainty()).isEqualTo(OperationalUncertainty.HIGH);
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionPermission() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(adapter(absent(EvidenceSourceType.EVENTS))),
				List.of(query(EvidenceSourceType.EVENTS, false))
		));

		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceCollectionRequest request = request(
				List.of(adapter(absent(EvidenceSourceType.ROLLBACK))),
				List.of(query(EvidenceSourceType.ROLLBACK, false))
		);
		EvidenceCollectionResult result = orchestrator.collect(request);

		assertThat(request.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectMissingAdapters() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(),
				List.of()
		));

		assertThat(result.rejectionReason()).isEqualTo(
				EvidenceCollectionRejectionReason.NO_ADAPTERS_CONFIGURED
		);
	}

	@Test
	void shouldRejectAdapterQuerySizeMismatch() {
		EvidenceCollectionResult result = orchestrator.collect(request(
				List.of(adapter(absent(EvidenceSourceType.METRICS))),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.LOGS, false)
				)
		));

		assertThat(result.rejectionReason()).isEqualTo(
				EvidenceCollectionRejectionReason.ADAPTER_QUERY_SIZE_MISMATCH
		);
	}

	@Test
	void shouldRejectNullRequest() {
		assertThatThrownBy(() -> orchestrator.collect(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("request must not be null");
	}

	private EvidenceCollectionRequest request(
			List<EvidenceAdapterPort> adapters,
			List<EvidenceQuery> queries
	) {
		return new EvidenceCollectionRequest(adapters, queries);
	}

	private EvidenceAdapterPort adapter(EvidenceQueryResult result) {
		return query -> result;
	}

	private EvidenceQuery query(
			EvidenceSourceType sourceType,
			boolean paymentRelated
	) {
		return new EvidenceQuery(
				sourceType,
				"incident-1",
				Instant.parse("2026-05-29T00:00:00Z"),
				Instant.parse("2026-05-29T01:00:00Z"),
				paymentRelated
		);
	}

	private EvidenceQueryResult collected(
			EvidenceSourceType sourceType,
			EvidenceSignal signal
	) {
		return new EvidenceQueryResult(
				sourceType,
				EvidenceCollectionStatus.COLLECTED,
				List.of(signal),
				false
		);
	}

	private EvidenceQueryResult absent(EvidenceSourceType sourceType) {
		return new EvidenceQueryResult(
				sourceType,
				EvidenceCollectionStatus.ABSENT,
				List.of(),
				false
		);
	}

	private EvidenceQueryResult unknown(EvidenceSourceType sourceType) {
		return new EvidenceQueryResult(
				sourceType,
				EvidenceCollectionStatus.UNKNOWN,
				List.of(),
				false
		);
	}

	private EvidenceQueryResult failed(EvidenceSourceType sourceType) {
		return new EvidenceQueryResult(
				sourceType,
				EvidenceCollectionStatus.FAILED,
				List.of(),
				false
		);
	}

	private EvidenceSignal signal(
			EvidenceSignalType type,
			String signalId,
			String summary
	) {
		return new EvidenceSignal(type, signalId, summary);
	}
}
