package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationMode;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationModeProperties;

import reactor.core.publisher.Mono;

class GovernanceTimelineProjectionQueryRuntimeSummaryServiceTest {

	private final Clock fixedClock = Clock.fixed(
			Instant.parse("2026-05-18T00:00:00Z"),
			ZoneOffset.UTC
	);

	@Test
	void shouldMapHealthyToNormal() {
		CountingHealthService healthService = new CountingHealthService(
				health(
						GovernanceTimelineProjectionQueryHealthStatus.HEALTHY,
						GovernanceTimelineAggregationMode.PROJECTION_BACKED,
						true
				)
		);

		GovernanceTimelineProjectionQueryRuntimeSummary summary = service(
				healthService
		).summary().block();

		assertThat(summary).isNotNull();
		assertThat(summary.checkedAt()).isEqualTo(Instant.parse("2026-05-18T00:00:00Z"));
		assertThat(summary.runtimeMode())
				.isEqualTo(GovernanceTimelineProjectionQueryRuntimeMode.NORMAL);
		assertThat(summary.aggregationMode())
				.isEqualTo(GovernanceTimelineAggregationMode.PROJECTION_BACKED);
		assertThat(summary.projectionBackedAvailable()).isTrue();
		assertThat(summary.lightweight()).isTrue();
		assertThat(summary.signals()).containsExactly(
				"projection-query:HEALTHY",
				"aggregation-mode:PROJECTION_BACKED"
		);
		assertThat(summary.summary())
				.isEqualTo("Projection-backed timeline query runtime is normal.");
		assertThat(healthService.healthInvocations()).isEqualTo(1);
	}

	@Test
	void shouldMapDegradedToDegradedReadOnly() {
		CountingHealthService healthService = new CountingHealthService(
				health(
						GovernanceTimelineProjectionQueryHealthStatus.DEGRADED,
						GovernanceTimelineAggregationMode.PROJECTION_BACKED,
						true
				)
		);

		GovernanceTimelineProjectionQueryRuntimeSummary summary = service(
				healthService
		).summary().block();

		assertThat(summary).isNotNull();
		assertThat(summary.runtimeMode())
				.isEqualTo(
						GovernanceTimelineProjectionQueryRuntimeMode.DEGRADED_READ_ONLY
				);
		assertThat(summary.signals()).containsExactly(
				"projection-query:DEGRADED",
				"aggregation-mode:PROJECTION_BACKED"
		);
		assertThat(summary.summary()).isEqualTo(
				"Projection-backed timeline query runtime is degraded but read-only safe."
		);
	}

	@Test
	void shouldMapUnavailableToAttentionRequired() {
		CountingHealthService healthService = new CountingHealthService(
				health(
						GovernanceTimelineProjectionQueryHealthStatus.UNAVAILABLE,
						GovernanceTimelineAggregationMode.RUNTIME_FAN_OUT,
						false
				)
		);

		GovernanceTimelineProjectionQueryRuntimeSummary summary = service(
				healthService
		).summary().block();

		assertThat(summary).isNotNull();
		assertThat(summary.runtimeMode())
				.isEqualTo(
						GovernanceTimelineProjectionQueryRuntimeMode.ATTENTION_REQUIRED
				);
		assertThat(summary.aggregationMode())
				.isEqualTo(GovernanceTimelineAggregationMode.RUNTIME_FAN_OUT);
		assertThat(summary.projectionBackedAvailable()).isFalse();
		assertThat(summary.lightweight()).isTrue();
		assertThat(summary.signals()).containsExactly(
				"projection-query:UNAVAILABLE",
				"aggregation-mode:RUNTIME_FAN_OUT"
		);
		assertThat(summary.summary()).isEqualTo(
				"Projection-backed timeline query runtime requires operational attention."
		);
	}

	@Test
	void shouldRejectNullHealthService() {
		assertThatThrownBy(() -> new GovernanceTimelineProjectionQueryRuntimeSummaryService(
				null
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("healthService must not be null");
	}

	private GovernanceTimelineProjectionQueryRuntimeSummaryService service(
			CountingHealthService healthService
	) {
		return new GovernanceTimelineProjectionQueryRuntimeSummaryService(healthService);
	}

	private GovernanceTimelineProjectionQueryHealthResponse health(
			GovernanceTimelineProjectionQueryHealthStatus status,
			GovernanceTimelineAggregationMode mode,
			boolean projectionBackedAvailable
	) {
		return new GovernanceTimelineProjectionQueryHealthResponse(
				Instant.now(fixedClock),
				status,
				mode,
				projectionBackedAvailable,
				true,
				"health-message"
		);
	}

	private static final class CountingHealthService
			extends GovernanceTimelineProjectionQueryHealthService {

		private final GovernanceTimelineProjectionQueryHealthResponse response;
		private final AtomicInteger healthInvocations = new AtomicInteger();

		private CountingHealthService(
				GovernanceTimelineProjectionQueryHealthResponse response
		) {
			super(
					null,
					properties(response.aggregationMode()),
					Clock.fixed(response.checkedAt(), ZoneOffset.UTC)
			);
			this.response = response;
		}

		@Override
		public Mono<GovernanceTimelineProjectionQueryHealthResponse> health() {
			healthInvocations.incrementAndGet();
			return Mono.just(response);
		}

		private int healthInvocations() {
			return healthInvocations.get();
		}

		private static GovernanceTimelineAggregationModeProperties properties(
				GovernanceTimelineAggregationMode mode
		) {
			GovernanceTimelineAggregationModeProperties properties =
					new GovernanceTimelineAggregationModeProperties();
			properties.setMode(mode);
			return properties;
		}
	}
}
