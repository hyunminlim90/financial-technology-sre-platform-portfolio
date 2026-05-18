package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceDetailHealthServiceTest {

	@Test
	void shouldReturnHealthyWhenResilienceIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailHealthService service =
				new GovernanceDetailHealthService(
						properties(false, true, true, 1500),
						metricsRecorder(registry)
				);

		GovernanceDetailHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceDetailHealthStatus.HEALTHY);
		assertThat(response.resilienceEnabled()).isFalse();
		assertThat(response.partialResponseEnabled()).isTrue();
		assertThat(response.failOpenDetail()).isTrue();
		assertThat(response.componentQueryTimeoutMs()).isEqualTo(1500);
		assertThat(registry.find(GovernanceDetailMetricName.HEALTH_STATUS)
				.gauge()
				.value()).isEqualTo(0.0);
	}

	@Test
	void shouldReturnDegradedCapableWhenPartialResponseAndFailOpenAreEnabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailHealthService service =
				new GovernanceDetailHealthService(
						properties(true, true, true, 2000),
						metricsRecorder(registry)
				);

		GovernanceDetailHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(
				GovernanceDetailHealthStatus.DEGRADED_CAPABLE
		);
		assertThat(response.resilienceEnabled()).isTrue();
		assertThat(response.partialResponseEnabled()).isTrue();
		assertThat(response.failOpenDetail()).isTrue();
		assertThat(response.componentQueryTimeoutMs()).isEqualTo(2000);
		assertThat(registry.find(GovernanceDetailMetricName.HEALTH_STATUS)
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnStrictWhenPartialResponseIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailHealthService service =
				new GovernanceDetailHealthService(
						properties(true, false, true, 1500),
						metricsRecorder(registry)
				);

		GovernanceDetailHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceDetailHealthStatus.STRICT);
		assertThat(response.partialResponseEnabled()).isFalse();
		assertThat(registry.find(GovernanceDetailMetricName.HEALTH_STATUS)
				.gauge()
				.value()).isEqualTo(2.0);
	}

	@Test
	void shouldReturnStrictWhenFailOpenIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailHealthService service =
				new GovernanceDetailHealthService(
						properties(true, true, false, 1500),
						metricsRecorder(registry)
				);

		GovernanceDetailHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceDetailHealthStatus.STRICT);
		assertThat(response.failOpenDetail()).isFalse();
		assertThat(registry.find(GovernanceDetailMetricName.HEALTH_STATUS)
				.gauge()
				.value()).isEqualTo(2.0);
	}

	private GovernanceDetailHealthMetricsRecorder metricsRecorder(
			SimpleMeterRegistry registry
	) {
		return new GovernanceDetailHealthMetricsRecorder(registry);
	}

	private GovernanceDetailResilienceProperties properties(
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
		return properties;
	}
}
