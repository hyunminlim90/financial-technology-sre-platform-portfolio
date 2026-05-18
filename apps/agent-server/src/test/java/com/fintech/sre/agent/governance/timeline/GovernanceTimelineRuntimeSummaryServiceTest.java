package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

class GovernanceTimelineRuntimeSummaryServiceTest {

	@Test
	void shouldMapHealthyToNormal() {
		GovernanceTimelineRuntimeSummaryResponse response = service(
				health(GovernanceTimelineHealthStatus.HEALTHY,
						GovernanceTimelineResilienceMode.STRICT,
						false,
						false)
		).summary().block();

		assertThat(response.runtimeMode()).isEqualTo(
				GovernanceTimelineRuntimeMode.NORMAL
		);
		assertThat(response.degradedSignals()).isEmpty();
	}

	@Test
	void shouldMapDegradedCapableToDegradedReadOnly() {
		GovernanceTimelineRuntimeSummaryResponse response = service(
				health(GovernanceTimelineHealthStatus.DEGRADED_CAPABLE,
						GovernanceTimelineResilienceMode.FAIL_OPEN_READ_ONLY,
						true,
						true)
		).summary().block();

		assertThat(response.runtimeMode()).isEqualTo(
				GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY
		);
		assertThat(response.degradedSignals()).contains(
				"timeline:DEGRADED_CAPABLE",
				"timeline:FAIL_OPEN_READ_ONLY"
		);
	}

	@Test
	void shouldMapStrictToAttentionRequired() {
		GovernanceTimelineRuntimeSummaryResponse response = service(
				health(GovernanceTimelineHealthStatus.STRICT,
						GovernanceTimelineResilienceMode.STRICT,
						false,
						false)
		).summary().block();

		assertThat(response.runtimeMode()).isEqualTo(
				GovernanceTimelineRuntimeMode.ATTENTION_REQUIRED
		);
		assertThat(response.degradedSignals()).contains("timeline:STRICT");
	}

	@Test
	void shouldMapUnavailableToAttentionRequired() {
		GovernanceTimelineRuntimeSummaryResponse response = service(
				health(GovernanceTimelineHealthStatus.UNAVAILABLE,
						GovernanceTimelineResilienceMode.STRICT,
						false,
						false)
		).summary().block();

		assertThat(response.runtimeMode()).isEqualTo(
				GovernanceTimelineRuntimeMode.ATTENTION_REQUIRED
		);
		assertThat(response.degradedSignals()).contains("timeline:UNAVAILABLE");
	}

	private GovernanceTimelineRuntimeSummaryService service(
			GovernanceTimelineHealthResponse response
	) {
		return new GovernanceTimelineRuntimeSummaryService(
				new StubTimelineHealthService(response),
				new GovernanceTimelineRuntimeMetricsRecorder(
						new SimpleMeterRegistry()
				)
		);
	}

	private GovernanceTimelineHealthResponse health(
			GovernanceTimelineHealthStatus status,
			GovernanceTimelineResilienceMode mode,
			boolean partial,
			boolean failOpen
	) {
		return new GovernanceTimelineHealthResponse(
				Instant.parse("2026-05-15T00:00:00Z"),
				status,
				mode,
				partial,
				failOpen,
				true,
				List.of("component_query_failed"),
				"health"
		);
	}

	private static final class StubTimelineHealthService
			extends GovernanceTimelineHealthService {

		private final GovernanceTimelineHealthResponse response;

		StubTimelineHealthService(GovernanceTimelineHealthResponse response) {
			super(new org.springframework.beans.factory.ObjectProvider<>() {
				@Override
				public GovernanceTimelineAggregationService getObject(Object... args) {
					return null;
				}

				@Override
				public GovernanceTimelineAggregationService getIfAvailable() {
					return null;
				}

				@Override
				public GovernanceTimelineAggregationService getIfUnique() {
					return null;
				}

				@Override
				public GovernanceTimelineAggregationService getObject() {
					return null;
				}
			},
					new GovernanceTimelineResilienceProperties(),
					new GovernanceTimelineHealthMetricsRecorder(new SimpleMeterRegistry()));
			this.response = response;
		}

		@Override
		public Mono<GovernanceTimelineHealthResponse> health() {
			return Mono.just(response);
		}
	}
}
