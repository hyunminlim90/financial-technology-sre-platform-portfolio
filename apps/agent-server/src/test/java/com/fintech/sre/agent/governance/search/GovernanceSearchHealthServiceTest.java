package com.fintech.sre.agent.governance.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceSearchHealthServiceTest {

	@Test
	void shouldReturnHealthyWhenResilienceIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchHealthService service = new GovernanceSearchHealthService(
				properties(false, true, true, 1500),
				metricsRecorder(registry)
		);

		GovernanceSearchHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceSearchHealthStatus.HEALTHY);
		assertThat(response.resilienceEnabled()).isFalse();
		assertThat(response.partialSearchEnabled()).isTrue();
		assertThat(response.failOpenSearch()).isTrue();
		assertThat(response.componentQueryTimeoutMs()).isEqualTo(1500);
		assertThat(registry.find(GovernanceSearchMetricName.HEALTH_STATUS)
				.tag("component", "governance-search")
				.gauge()
				.value()).isEqualTo(0.0);
	}

	@Test
	void shouldReturnDegradedCapableWhenPartialSearchAndFailOpenAreEnabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchHealthService service = new GovernanceSearchHealthService(
				properties(true, true, true, 2000),
				metricsRecorder(registry)
		);

		GovernanceSearchHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(
				GovernanceSearchHealthStatus.DEGRADED_CAPABLE
		);
		assertThat(response.resilienceEnabled()).isTrue();
		assertThat(response.partialSearchEnabled()).isTrue();
		assertThat(response.failOpenSearch()).isTrue();
		assertThat(response.componentQueryTimeoutMs()).isEqualTo(2000);
		assertThat(registry.find(GovernanceSearchMetricName.HEALTH_STATUS)
				.tag("component", "governance-search")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnStrictWhenPartialSearchIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchHealthService service = new GovernanceSearchHealthService(
				properties(true, false, true, 1500),
				metricsRecorder(registry)
		);

		GovernanceSearchHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceSearchHealthStatus.STRICT);
		assertThat(response.partialSearchEnabled()).isFalse();
		assertThat(registry.find(GovernanceSearchMetricName.HEALTH_STATUS)
				.tag("component", "governance-search")
				.gauge()
				.value()).isEqualTo(2.0);
	}

	@Test
	void shouldReturnStrictWhenFailOpenIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchHealthService service = new GovernanceSearchHealthService(
				properties(true, true, false, 1500),
				metricsRecorder(registry)
		);

		GovernanceSearchHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceSearchHealthStatus.STRICT);
		assertThat(response.failOpenSearch()).isFalse();
		assertThat(registry.find(GovernanceSearchMetricName.HEALTH_STATUS)
				.tag("component", "governance-search")
				.gauge()
				.value()).isEqualTo(2.0);
	}

	private GovernanceSearchHealthMetricsRecorder metricsRecorder(
			SimpleMeterRegistry registry
	) {
		return new GovernanceSearchHealthMetricsRecorder(registry);
	}

	private GovernanceSearchResilienceProperties properties(
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
		return properties;
	}
}
