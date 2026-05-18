package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceTimelineHealthServiceTest {

	@Test
	void shouldReturnHealthyWhenResilienceIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineHealthService service = new GovernanceTimelineHealthService(
				provider(new StubAggregationService()),
				properties(false, true, true, 1500),
				new GovernanceTimelineHealthMetricsRecorder(registry)
		);

		GovernanceTimelineHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceTimelineHealthStatus.HEALTHY);
		assertThat(response.resilienceMode()).isEqualTo(
				GovernanceTimelineResilienceMode.STRICT
		);
		assertThat(response.partialTimelineSupported()).isFalse();
		assertThat(response.failOpenReadOnly()).isFalse();
		assertThat(response.streamingCompatible()).isTrue();
		assertThat(response.degradedReasonTaxonomy()).containsExactly(
				"component_query_failed",
				"component_query_timeout",
				"projection_failed",
				"aggregation_degraded",
				"timeline_query_timeout"
		);
	}

	@Test
	void shouldReturnDegradedCapableWhenFailOpenReadOnlyIsEnabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineHealthService service = new GovernanceTimelineHealthService(
				provider(new StubAggregationService()),
				properties(true, true, true, 1500),
				new GovernanceTimelineHealthMetricsRecorder(registry)
		);

		GovernanceTimelineHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(
				GovernanceTimelineHealthStatus.DEGRADED_CAPABLE
		);
		assertThat(response.resilienceMode()).isEqualTo(
				GovernanceTimelineResilienceMode.FAIL_OPEN_READ_ONLY
		);
		assertThat(response.partialTimelineSupported()).isTrue();
		assertThat(response.failOpenReadOnly()).isTrue();
	}

	@Test
	void shouldReturnStrictWhenPartialTimelineIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineHealthService service = new GovernanceTimelineHealthService(
				provider(new StubAggregationService()),
				properties(true, false, true, 1500),
				new GovernanceTimelineHealthMetricsRecorder(registry)
		);

		GovernanceTimelineHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceTimelineHealthStatus.STRICT);
		assertThat(response.resilienceMode()).isEqualTo(
				GovernanceTimelineResilienceMode.STRICT
		);
		assertThat(response.partialTimelineSupported()).isFalse();
		assertThat(response.failOpenReadOnly()).isFalse();
	}

	@Test
	void shouldReturnStrictWhenFailOpenReadOnlyIsDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineHealthService service = new GovernanceTimelineHealthService(
				provider(new StubAggregationService()),
				properties(true, true, false, 1500),
				new GovernanceTimelineHealthMetricsRecorder(registry)
		);

		GovernanceTimelineHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceTimelineHealthStatus.STRICT);
		assertThat(response.resilienceMode()).isEqualTo(
				GovernanceTimelineResilienceMode.PARTIAL_DEGRADED
		);
		assertThat(response.partialTimelineSupported()).isTrue();
		assertThat(response.failOpenReadOnly()).isFalse();
	}

	@Test
	void shouldReturnUnavailableWhenAggregationServiceMissing() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineHealthService service = new GovernanceTimelineHealthService(
				provider(null),
				properties(false, true, true, 1500),
				new GovernanceTimelineHealthMetricsRecorder(registry)
		);

		GovernanceTimelineHealthResponse response = service.health().block();

		assertThat(response.status()).isEqualTo(GovernanceTimelineHealthStatus.UNAVAILABLE);
	}

	private GovernanceTimelineResilienceProperties properties(
			boolean enabled,
			boolean partialTimelineEnabled,
			boolean failOpenReadOnly,
			int componentQueryTimeoutMs
	) {
		GovernanceTimelineResilienceProperties properties =
				new GovernanceTimelineResilienceProperties();
		properties.setEnabled(enabled);
		properties.setPartialTimelineEnabled(partialTimelineEnabled);
		properties.setFailOpenReadOnly(failOpenReadOnly);
		properties.setComponentQueryTimeoutMs(componentQueryTimeoutMs);
		return properties;
	}

	private ObjectProvider<GovernanceTimelineAggregationService> provider(
			GovernanceTimelineAggregationService service
	) {
		return new ObjectProvider<>() {
			@Override
			public GovernanceTimelineAggregationService getObject(Object... args) {
				return service;
			}

			@Override
			public GovernanceTimelineAggregationService getIfAvailable() {
				return service;
			}

			@Override
			public GovernanceTimelineAggregationService getIfUnique() {
				return service;
			}

			@Override
			public GovernanceTimelineAggregationService getObject() {
				return service;
			}
		};
	}

	private static final class StubAggregationService
			implements GovernanceTimelineAggregationService {

		@Override
		public reactor.core.publisher.Mono<GovernanceTimelineAggregationResult> aggregate(
				GovernanceTimelineAggregationRequest request
		) {
			return reactor.core.publisher.Mono.empty();
		}
	}
}
