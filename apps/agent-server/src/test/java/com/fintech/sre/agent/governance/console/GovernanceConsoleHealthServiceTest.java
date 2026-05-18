package com.fintech.sre.agent.governance.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardBucketSize;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthMetricsRecorder;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthService;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthStatus;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardTimeRange;
import com.fintech.sre.agent.governance.detail.GovernanceDetailHealthMetricsRecorder;
import com.fintech.sre.agent.governance.detail.GovernanceDetailHealthService;
import com.fintech.sre.agent.governance.detail.GovernanceDetailResilienceProperties;
import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryRepository;
import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryResult;
import com.fintech.sre.agent.governance.query.GovernanceDashboardTimeBucketResult;
import com.fintech.sre.agent.governance.query.GovernanceQueryResilienceProperties;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthMetricsRecorder;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthService;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthStatus;
import com.fintech.sre.agent.governance.search.GovernanceSearchResilienceProperties;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;

class GovernanceConsoleHealthServiceTest {

	@Test
	void shouldReturnHealthyWhenAllComponentsAreHealthy() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleHealthService service = service(
				dashboardHealthService(true, true, true, false),
				detailHealthService(false, true, true, 1500),
				searchHealthService(false, true, true, 1500),
				registry
		);

		GovernanceConsoleHealthResponse response = service.health().block();

		assertThat(response.overallStatus()).isEqualTo(GovernanceConsoleHealthStatus.HEALTHY);
		assertThat(response.dashboardHealth().status()).isEqualTo(
				GovernanceDashboardHealthStatus.HEALTHY
		);
		assertThat(response.detailHealth().status()).isEqualTo(
				com.fintech.sre.agent.governance.detail.GovernanceDetailHealthStatus.HEALTHY
		);
		assertThat(response.searchHealth().status()).isEqualTo(
				GovernanceSearchHealthStatus.HEALTHY
		);
		assertThat(registry.find(GovernanceConsoleMetricName.HEALTH_STATUS)
				.tag("component", "governance-console")
				.gauge()
				.value()).isEqualTo(0.0);
	}

	@Test
	void shouldReturnDegradedWhenDashboardIsDegraded() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleHealthService service = service(
				dashboardHealthService(false, true, true, true),
				detailHealthService(false, true, true, 1500),
				searchHealthService(false, true, true, 1500),
				registry
		);

		GovernanceConsoleHealthResponse response = service.health().block();

		assertThat(response.overallStatus()).isEqualTo(GovernanceConsoleHealthStatus.DEGRADED);
		assertThat(response.dashboardHealth().status()).isEqualTo(
				GovernanceDashboardHealthStatus.DEGRADED
		);
		assertThat(registry.find(GovernanceConsoleMetricName.HEALTH_STATUS)
				.tag("component", "governance-console")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnDegradedWhenDetailIsDegradedCapable() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleHealthService service = service(
				dashboardHealthService(true, true, true, false),
				detailHealthService(true, true, true, 1500),
				searchHealthService(false, true, true, 1500),
				registry
		);

		GovernanceConsoleHealthResponse response = service.health().block();

		assertThat(response.overallStatus()).isEqualTo(GovernanceConsoleHealthStatus.DEGRADED);
		assertThat(response.detailHealth().status().name()).isEqualTo("DEGRADED_CAPABLE");
		assertThat(registry.find(GovernanceConsoleMetricName.HEALTH_STATUS)
				.tag("component", "governance-console")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnDegradedWhenSearchIsDegradedCapable() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleHealthService service = service(
				dashboardHealthService(true, true, true, false),
				detailHealthService(false, true, true, 1500),
				searchHealthService(true, true, true, 1500),
				registry
		);

		GovernanceConsoleHealthResponse response = service.health().block();

		assertThat(response.overallStatus()).isEqualTo(GovernanceConsoleHealthStatus.DEGRADED);
		assertThat(response.searchHealth().status()).isEqualTo(
				GovernanceSearchHealthStatus.DEGRADED_CAPABLE
		);
		assertThat(registry.find(GovernanceConsoleMetricName.HEALTH_STATUS)
				.tag("component", "governance-console")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnAttentionRequiredWhenDashboardIsUnavailable() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleHealthService service = service(
				dashboardHealthService(false, false, false, true),
				detailHealthService(false, true, true, 1500),
				searchHealthService(false, true, true, 1500),
				registry
		);

		GovernanceConsoleHealthResponse response = service.health().block();

		assertThat(response.overallStatus()).isEqualTo(
				GovernanceConsoleHealthStatus.ATTENTION_REQUIRED
		);
		assertThat(response.dashboardHealth().status()).isEqualTo(
				GovernanceDashboardHealthStatus.UNAVAILABLE
		);
		assertThat(registry.find(GovernanceConsoleMetricName.HEALTH_STATUS)
				.tag("component", "governance-console")
				.gauge()
				.value()).isEqualTo(2.0);
	}

	@Test
	void shouldReturnAttentionRequiredWhenDetailIsStrict() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleHealthService service = service(
				dashboardHealthService(true, true, true, false),
				detailHealthService(true, false, true, 1500),
				searchHealthService(false, true, true, 1500),
				registry
		);

		GovernanceConsoleHealthResponse response = service.health().block();

		assertThat(response.overallStatus()).isEqualTo(
				GovernanceConsoleHealthStatus.ATTENTION_REQUIRED
		);
		assertThat(response.detailHealth().status().name()).isEqualTo("STRICT");
		assertThat(registry.find(GovernanceConsoleMetricName.HEALTH_STATUS)
				.tag("component", "governance-console")
				.gauge()
				.value()).isEqualTo(2.0);
	}

	@Test
	void shouldReturnAttentionRequiredWhenSearchIsStrict() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleHealthService service = service(
				dashboardHealthService(true, true, true, false),
				detailHealthService(false, true, true, 1500),
				searchHealthService(true, false, true, 1500),
				registry
		);

		GovernanceConsoleHealthResponse response = service.health().block();

		assertThat(response.overallStatus()).isEqualTo(
				GovernanceConsoleHealthStatus.ATTENTION_REQUIRED
		);
		assertThat(response.searchHealth().status()).isEqualTo(
				GovernanceSearchHealthStatus.STRICT
		);
		assertThat(registry.find(GovernanceConsoleMetricName.HEALTH_STATUS)
				.tag("component", "governance-console")
				.gauge()
				.value()).isEqualTo(2.0);
	}

	private GovernanceConsoleHealthService service(
			GovernanceDashboardHealthService dashboardHealthService,
			GovernanceDetailHealthService detailHealthService,
			GovernanceSearchHealthService searchHealthService,
			SimpleMeterRegistry registry
	) {
		return new GovernanceConsoleHealthService(
				dashboardHealthService,
				detailHealthService,
				searchHealthService,
				new GovernanceConsoleHealthMetricsRecorder(registry)
		);
	}

	private GovernanceDashboardHealthService dashboardHealthService(
			boolean repositoryAvailable,
			boolean fallbackEnabled,
			boolean failOpenDashboard,
			boolean resilienceEnabled
	) {
		GovernanceQueryResilienceProperties properties =
				new GovernanceQueryResilienceProperties();
		properties.setFallbackEnabled(fallbackEnabled);
		properties.setFailOpenDashboard(failOpenDashboard);
		properties.setEnabled(resilienceEnabled);

		return new GovernanceDashboardHealthService(
				repositoryAvailable ? Optional.of(emptyRepository()) : Optional.empty(),
				properties,
				new GovernanceDashboardHealthMetricsRecorder(new SimpleMeterRegistry())
		);
	}

	private GovernanceDetailHealthService detailHealthService(
			boolean enabled,
			boolean partialResponseEnabled,
			boolean failOpenDetail,
			int componentQueryTimeoutMs
	) {
		GovernanceDetailResilienceProperties properties =
				new GovernanceDetailResilienceProperties();
		properties.setEnabled(enabled);
		properties.setPartialResponseEnabled(partialResponseEnabled);
		properties.setFailOpenDetail(failOpenDetail);
		properties.setComponentQueryTimeoutMs(componentQueryTimeoutMs);

		return new GovernanceDetailHealthService(
				properties,
				new GovernanceDetailHealthMetricsRecorder(new SimpleMeterRegistry())
		);
	}

	private GovernanceSearchHealthService searchHealthService(
			boolean enabled,
			boolean partialSearchEnabled,
			boolean failOpenSearch,
			int componentQueryTimeoutMs
	) {
		GovernanceSearchResilienceProperties properties =
				new GovernanceSearchResilienceProperties();
		properties.setEnabled(enabled);
		properties.setPartialSearchEnabled(partialSearchEnabled);
		properties.setFailOpenSearch(failOpenSearch);
		properties.setComponentQueryTimeoutMs(componentQueryTimeoutMs);

		return new GovernanceSearchHealthService(
				properties,
				new GovernanceSearchHealthMetricsRecorder(new SimpleMeterRegistry())
		);
	}

	private GovernanceDashboardQueryRepository emptyRepository() {
		return new GovernanceDashboardQueryRepository() {
			@Override
			public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardQueryResult> findVerificationStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardQueryResult> findLatestIncidentStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardTimeBucketResult> findApprovalStatusBuckets(
					GovernanceDashboardTimeRange range,
					GovernanceDashboardBucketSize bucketSize
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardTimeBucketResult> findVerificationStatusBuckets(
					GovernanceDashboardTimeRange range,
					GovernanceDashboardBucketSize bucketSize
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardTimeBucketResult> findIncidentLifecycleStatusBuckets(
					GovernanceDashboardTimeRange range,
					GovernanceDashboardBucketSize bucketSize
			) {
				return Flux.empty();
			}
		};
	}
}
