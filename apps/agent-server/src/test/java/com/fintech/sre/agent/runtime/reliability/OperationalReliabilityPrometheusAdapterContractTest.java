package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityPrometheusAdapterContractTest {

	@Test
	void shouldRemainMetricsEvidenceSourceOnly() {
		PrometheusEvidenceQuery query = query(
				new EvidenceQuery(
						EvidenceSourceType.METRICS,
						"incident-1",
						Instant.parse("2026-05-29T00:00:00Z"),
						Instant.parse("2026-05-29T01:00:00Z"),
						false
				),
				"http_server_requests_seconds",
				PrometheusMetricSemanticType.LATENCY
		);

		assertThat(query.evidenceQuery().sourceType()).isEqualTo(EvidenceSourceType.METRICS);
		assertThat(query.metricsEvidenceOnly()).isTrue();
	}

	@Test
	void shouldRejectNonMetricsEvidenceSource() {
		assertThatThrownBy(() -> query(
				new EvidenceQuery(
						EvidenceSourceType.LOGS,
						"incident-1",
						Instant.parse("2026-05-29T00:00:00Z"),
						Instant.parse("2026-05-29T01:00:00Z"),
						false
				),
				"http_server_requests_seconds",
				PrometheusMetricSemanticType.LATENCY
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Prometheus evidence query requires METRICS source");
	}

	@Test
	void shouldNotExposeRawPrometheusResponse() {
		PrometheusEvidenceMapping mapping = mapping(
				"http_server_requests_seconds",
				PrometheusMetricSemanticType.LATENCY,
				false,
				false
		);

		assertThat(mapping.exposesRawPrometheusPayload()).isFalse();
		assertThat(query(metricsQuery(), "http_server_requests_seconds",
				PrometheusMetricSemanticType.LATENCY).exposesRawPrometheusPayload()).isFalse();
	}

	@Test
	void shouldConvertPrometheusMetricToNormalizedEvidenceSignal() {
		PrometheusEvidenceMapping mapping = mapping(
				"http_server_requests_seconds",
				PrometheusMetricSemanticType.LATENCY,
				false,
				false
		);

		assertThat(mapping.normalizedEvidenceOnly()).isTrue();
		assertThat(mapping.toEvidenceSignal().type()).isEqualTo(EvidenceSignalType.METRIC);
	}

	@Test
	void shouldKeepAdapterFailureAsUnknownOrFailedEvidenceInsteadOfSystemFailure() {
		PrometheusEvidenceAdapterContract adapter =
				new PrometheusEvidenceAdapterContract() {
					@Override
					public EvidenceQueryResult collect(PrometheusEvidenceQuery query) {
						return failed(query);
					}
				};

		EvidenceQueryResult failed = adapter.failed(
				query(metricsQuery(), "up", PrometheusMetricSemanticType.UNKNOWN)
		);
		EvidenceQueryResult unknown = adapter.unknown(
				query(metricsQuery(), "up", PrometheusMetricSemanticType.UNKNOWN)
		);

		assertThat(failed.status()).isEqualTo(EvidenceCollectionStatus.FAILED);
		assertThat(failed.systemFailure()).isFalse();
		assertThat(unknown.status()).isEqualTo(EvidenceCollectionStatus.UNKNOWN);
		assertThat(unknown.maintainsUncertainty()).isTrue();
	}

	@Test
	void shouldRejectHighCardinalityLabelsFromSemanticEvidenceExposure() {
		PrometheusEvidenceMapping mapping = mapping(
				"http_server_requests_seconds",
				PrometheusMetricSemanticType.LATENCY,
				false,
				true
		);

		assertThat(mapping.suppressesHighCardinalityLabels()).isFalse();
		assertThatThrownBy(mapping::toEvidenceSignal)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("high-cardinality labels must not be exposed as semantic evidence");
	}

	@Test
	void shouldNotElevatePaymentMetricWithoutConsistencyMetadata() {
		PrometheusEvidenceMapping mapping = mapping(
				"payment_consistency_mismatch_total",
				PrometheusMetricSemanticType.PAYMENT_CONSISTENCY,
				false,
				false
		);

		assertThat(mapping.paymentSafetyElevated()).isFalse();
		assertThat(mapping.toEvidenceSignal().type()).isEqualTo(EvidenceSignalType.METRIC);
	}

	@Test
	void shouldElevatePaymentMetricWhenConsistencyMetadataExists() {
		PrometheusEvidenceMapping mapping = mapping(
				"payment_consistency_mismatch_total",
				PrometheusMetricSemanticType.PAYMENT_CONSISTENCY,
				true,
				false
		);

		assertThat(mapping.paymentSafetyElevated()).isTrue();
		assertThat(mapping.toEvidenceSignal().type())
				.isEqualTo(EvidenceSignalType.PAYMENT_SAFETY);
	}

	@Test
	void shouldNotGrantExecutionOrRecommendationAuthority() {
		PrometheusEvidenceAdapterContract adapter =
				new PrometheusEvidenceAdapterContract() {
					@Override
					public EvidenceQueryResult collect(PrometheusEvidenceQuery query) {
						return unknown(query);
					}
				};

		assertThat(adapter.executionAuthority()).isFalse();
		assertThat(adapter.recommendationAuthority()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		PrometheusEvidenceAdapterContract adapter =
				new PrometheusEvidenceAdapterContract() {
					@Override
					public EvidenceQueryResult collect(PrometheusEvidenceQuery query) {
						return unknown(query);
					}
				};
		PrometheusEvidenceQuery query = query(
				metricsQuery(),
				"http_server_requests_seconds",
				PrometheusMetricSemanticType.LATENCY
		);

		assertThat(query.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(adapter.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeSupportedPrometheusMetricSemanticTypes() {
		assertThat(PrometheusMetricSemanticType.values()).containsExactly(
				PrometheusMetricSemanticType.LATENCY,
				PrometheusMetricSemanticType.ERROR_RATE,
				PrometheusMetricSemanticType.TRAFFIC,
				PrometheusMetricSemanticType.SATURATION,
				PrometheusMetricSemanticType.QUEUE_DEPTH,
				PrometheusMetricSemanticType.RETRY_RATE,
				PrometheusMetricSemanticType.RESOURCE_UTILIZATION,
				PrometheusMetricSemanticType.PAYMENT_CONSISTENCY,
				PrometheusMetricSemanticType.UNKNOWN
		);
		assertThat(PrometheusEvidenceRejectionReason.values()).containsExactly(
				PrometheusEvidenceRejectionReason.METRICS_SOURCE_REQUIRED,
				PrometheusEvidenceRejectionReason.HIGH_CARDINALITY_LABELS_FORBIDDEN,
				PrometheusEvidenceRejectionReason.PAYMENT_CONSISTENCY_METADATA_REQUIRED
		);
	}

	@Test
	void shouldReturnCollectedMetricsAsNormalizedSemanticEvidence() {
		PrometheusEvidenceAdapterContract adapter =
				new PrometheusEvidenceAdapterContract() {
					@Override
					public EvidenceQueryResult collect(PrometheusEvidenceQuery query) {
						return collected(
								query,
								List.of(mapping(
										"http_server_requests_seconds",
										PrometheusMetricSemanticType.LATENCY,
										false,
										false
								))
						);
					}
				};

		EvidenceQueryResult result = adapter.collect(query(
				metricsQuery(),
				"http_server_requests_seconds",
				PrometheusMetricSemanticType.LATENCY
		));

		assertThat(result.sourceType()).isEqualTo(EvidenceSourceType.METRICS);
		assertThat(result.normalizedSemanticEvidenceOnly()).isTrue();
		assertThat(result.signals()).extracting(EvidenceSignal::type)
				.containsExactly(EvidenceSignalType.METRIC);
	}

	private EvidenceQuery metricsQuery() {
		return new EvidenceQuery(
				EvidenceSourceType.METRICS,
				"incident-1",
				Instant.parse("2026-05-29T00:00:00Z"),
				Instant.parse("2026-05-29T01:00:00Z"),
				false
		);
	}

	private PrometheusEvidenceQuery query(
			EvidenceQuery evidenceQuery,
			String metricName,
			PrometheusMetricSemanticType semanticType
	) {
		return new PrometheusEvidenceQuery(
				evidenceQuery,
				metricName,
				semanticType
		);
	}

	private PrometheusEvidenceMapping mapping(
			String metricName,
			PrometheusMetricSemanticType semanticType,
			boolean paymentConsistencyMetadataPresent,
			boolean highCardinalityLabelsPresent
	) {
		return new PrometheusEvidenceMapping(
				metricName,
				semanticType,
				"signal-" + metricName,
				"summary-" + metricName,
				paymentConsistencyMetadataPresent,
				highCardinalityLabelsPresent,
				highCardinalityLabelsPresent
						? PrometheusEvidenceRejectionReason
								.HIGH_CARDINALITY_LABELS_FORBIDDEN
						: null
		);
	}
}
