package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryRepository;
import com.fintech.sre.agent.governance.query.GovernanceQueryResilienceProperties;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;

class GovernanceDashboardHealthServiceTest {

	@Test
	void shouldReturnHealthyWhenOptimizedRepositoryIsAvailable() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardHealthService service =
				new GovernanceDashboardHealthService(
						Optional.of(emptyRepository()),
						properties(false, true, true),
						metricsRecorder(registry)
				);

		GovernanceDashboardHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceDashboardHealthStatus.HEALTHY);
		assertThat(response.optimizedQueryAvailable()).isTrue();
		assertThat(response.lastDegradationReason()).isEqualTo("none");
		assertThat(registry.find(GovernanceDashboardMetricName.HEALTH_STATUS)
				.gauge()
				.value()).isEqualTo(0.0);
	}

	@Test
	void shouldReturnDegradedWhenRepositoryIsMissingAndFallbackIsAvailable() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardHealthService service =
				new GovernanceDashboardHealthService(
						Optional.empty(),
						properties(true, true, true),
						metricsRecorder(registry)
				);

		GovernanceDashboardHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceDashboardHealthStatus.DEGRADED);
		assertThat(response.optimizedQueryAvailable()).isFalse();
		assertThat(response.fallbackEnabled()).isTrue();
		assertThat(response.failOpenDashboard()).isTrue();
		assertThat(response.resilienceEnabled()).isTrue();
		assertThat(response.lastDegradationReason())
				.isEqualTo("optimized_query_repository_missing");
		assertThat(registry.find(GovernanceDashboardMetricName.HEALTH_STATUS)
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnUnavailableWhenRepositoryIsMissingAndFallbackIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardHealthService service =
				new GovernanceDashboardHealthService(
						Optional.empty(),
						properties(false, false, false),
						metricsRecorder(registry)
				);

		GovernanceDashboardHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceDashboardHealthStatus.UNAVAILABLE);
		assertThat(response.optimizedQueryAvailable()).isFalse();
		assertThat(response.fallbackEnabled()).isFalse();
		assertThat(response.failOpenDashboard()).isFalse();
		assertThat(response.lastDegradationReason())
				.isEqualTo("optimized_query_repository_missing");
		assertThat(registry.find(GovernanceDashboardMetricName.HEALTH_STATUS)
				.gauge()
				.value()).isEqualTo(2.0);
	}

	private GovernanceDashboardHealthMetricsRecorder metricsRecorder(
			SimpleMeterRegistry registry
	) {
		return new GovernanceDashboardHealthMetricsRecorder(registry);
	}

	private GovernanceQueryResilienceProperties properties(
			boolean enabled,
			boolean fallbackEnabled,
			boolean failOpenDashboard
	) {
		GovernanceQueryResilienceProperties properties =
				new GovernanceQueryResilienceProperties();
		properties.setEnabled(enabled);
		properties.setFallbackEnabled(fallbackEnabled);
		properties.setFailOpenDashboard(failOpenDashboard);
		return properties;
	}

	private GovernanceDashboardQueryRepository emptyRepository() {
		return new GovernanceDashboardQueryRepository() {
			@Override
			public Flux<com.fintech.sre.agent.governance.query.GovernanceDashboardQueryResult> findApprovalStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
			}

			@Override
			public Flux<com.fintech.sre.agent.governance.query.GovernanceDashboardQueryResult> findVerificationStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
			}

			@Override
			public Flux<com.fintech.sre.agent.governance.query.GovernanceDashboardQueryResult> findLatestIncidentStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
			}

			@Override
			public Flux<com.fintech.sre.agent.governance.query.GovernanceDashboardTimeBucketResult> findApprovalStatusBuckets(
					GovernanceDashboardTimeRange range,
					GovernanceDashboardBucketSize bucketSize
			) {
				return Flux.empty();
			}

			@Override
			public Flux<com.fintech.sre.agent.governance.query.GovernanceDashboardTimeBucketResult> findVerificationStatusBuckets(
					GovernanceDashboardTimeRange range,
					GovernanceDashboardBucketSize bucketSize
			) {
				return Flux.empty();
			}

			@Override
			public Flux<com.fintech.sre.agent.governance.query.GovernanceDashboardTimeBucketResult> findIncidentLifecycleStatusBuckets(
					GovernanceDashboardTimeRange range,
					GovernanceDashboardBucketSize bucketSize
			) {
				return Flux.empty();
			}
		};
	}
}
