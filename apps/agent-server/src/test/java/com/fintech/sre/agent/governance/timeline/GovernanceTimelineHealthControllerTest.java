package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

class GovernanceTimelineHealthControllerTest {

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		webTestClient = WebTestClient.bindToController(
				new GovernanceTimelineHealthController(
						new StubTimelineHealthService()
				)
		).configureClient().build();
	}

	@Test
	void shouldReturnTimelineHealth() {
		webTestClient.get()
				.uri("/internal/governance/timeline/health")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("HEALTHY")
				.jsonPath("$.resilienceMode").isEqualTo("STRICT")
				.jsonPath("$.partialTimelineSupported").isEqualTo(false)
				.jsonPath("$.streamingCompatible").isEqualTo(true);
	}

	private static final class StubTimelineHealthService
			extends GovernanceTimelineHealthService {

		StubTimelineHealthService() {
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
					new GovernanceTimelineHealthMetricsRecorder(
					new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
			));
		}

		@Override
		public Mono<GovernanceTimelineHealthResponse> health() {
			return Mono.just(new GovernanceTimelineHealthResponse(
					Instant.parse("2026-05-15T00:00:00Z"),
					GovernanceTimelineHealthStatus.HEALTHY,
					GovernanceTimelineResilienceMode.STRICT,
					false,
					false,
					true,
					List.of("component_query_failed"),
					"healthy"
			));
		}
	}
}
