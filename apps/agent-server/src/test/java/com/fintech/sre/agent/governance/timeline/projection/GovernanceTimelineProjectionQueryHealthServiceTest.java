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
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationRequest;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationResult;

import reactor.core.publisher.Mono;

class GovernanceTimelineProjectionQueryHealthServiceTest {

	private final Clock fixedClock = Clock.fixed(
			Instant.parse("2026-05-18T00:00:00Z"),
			ZoneOffset.UTC
	);

	@Test
	void shouldReturnHealthyWhenAdapterExists() {
		CountingQueryAdapter adapter = new CountingQueryAdapter();
		GovernanceTimelineProjectionQueryHealthService service =
				new GovernanceTimelineProjectionQueryHealthService(
						adapter,
						properties(GovernanceTimelineAggregationMode.PROJECTION_BACKED),
						fixedClock
				);

		GovernanceTimelineProjectionQueryHealthResponse response = service.health().block();

		assertThat(response).isNotNull();
		assertThat(response.checkedAt()).isEqualTo(Instant.parse("2026-05-18T00:00:00Z"));
		assertThat(response.status())
				.isEqualTo(GovernanceTimelineProjectionQueryHealthStatus.HEALTHY);
		assertThat(response.aggregationMode())
				.isEqualTo(GovernanceTimelineAggregationMode.PROJECTION_BACKED);
		assertThat(response.projectionBackedAvailable()).isTrue();
		assertThat(response.lightweight()).isTrue();
		assertThat(response.message())
				.isEqualTo("Projection-backed timeline query path is available.");
		assertThat(adapter.invocations()).isZero();
	}

	@Test
	void shouldReturnUnavailableWhenAdapterMissing() {
		GovernanceTimelineProjectionQueryHealthService service =
				new GovernanceTimelineProjectionQueryHealthService(
						null,
						properties(GovernanceTimelineAggregationMode.RUNTIME_FAN_OUT),
						fixedClock
				);

		GovernanceTimelineProjectionQueryHealthResponse response = service.health().block();

		assertThat(response).isNotNull();
		assertThat(response.status())
				.isEqualTo(GovernanceTimelineProjectionQueryHealthStatus.UNAVAILABLE);
		assertThat(response.aggregationMode())
				.isEqualTo(GovernanceTimelineAggregationMode.RUNTIME_FAN_OUT);
		assertThat(response.projectionBackedAvailable()).isFalse();
		assertThat(response.lightweight()).isTrue();
		assertThat(response.message())
				.isEqualTo("Projection-backed timeline query path is unavailable.");
	}

	@Test
	void shouldRejectNullProperties() {
		assertThatThrownBy(() -> new GovernanceTimelineProjectionQueryHealthService(
				new CountingQueryAdapter(),
				null,
				fixedClock
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("properties must not be null");
	}

	@Test
	void shouldRejectNullClock() {
		assertThatThrownBy(() -> new GovernanceTimelineProjectionQueryHealthService(
				new CountingQueryAdapter(),
				properties(GovernanceTimelineAggregationMode.PROJECTION_BACKED),
				null
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("clock must not be null");
	}

	private GovernanceTimelineAggregationModeProperties properties(
			GovernanceTimelineAggregationMode mode
	) {
		GovernanceTimelineAggregationModeProperties properties =
				new GovernanceTimelineAggregationModeProperties();
		properties.setMode(mode);
		return properties;
	}

	private static final class CountingQueryAdapter
			implements GovernanceTimelineProjectionQueryAdapter {

		private final AtomicInteger invocations = new AtomicInteger();

		@Override
		public Mono<GovernanceTimelineAggregationResult> query(
				GovernanceTimelineAggregationRequest request
		) {
			invocations.incrementAndGet();
			return Mono.just(new GovernanceTimelineAggregationResult(
					null,
					false,
					List.of(),
					"none"
			));
		}

		private int invocations() {
			return invocations.get();
		}
	}
}
