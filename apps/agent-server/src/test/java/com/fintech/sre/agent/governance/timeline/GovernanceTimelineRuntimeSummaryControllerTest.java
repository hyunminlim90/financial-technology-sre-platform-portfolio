package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

class GovernanceTimelineRuntimeSummaryControllerTest {

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		webTestClient = WebTestClient.bindToController(
				new GovernanceTimelineRuntimeSummaryController(
						new StubRuntimeSummaryService()
				)
		).configureClient().build();
	}

	@Test
	void shouldReturnTimelineRuntimeSummary() {
		webTestClient.get()
				.uri("/internal/governance/timeline/runtime-summary")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.runtimeMode").isEqualTo("DEGRADED_READ_ONLY")
				.jsonPath("$.healthStatus").isEqualTo("DEGRADED_CAPABLE")
				.jsonPath("$.degradedSignals[0]").isEqualTo("timeline:DEGRADED_CAPABLE");
	}

	private static final class StubRuntimeSummaryService
			extends GovernanceTimelineRuntimeSummaryService {

		StubRuntimeSummaryService() {
			super(
					new GovernanceTimelineHealthService(
							new org.springframework.beans.factory.ObjectProvider<>() {
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
									new SimpleMeterRegistry()
							)
					),
					new GovernanceTimelineRuntimeMetricsRecorder(
							new SimpleMeterRegistry()
					)
			);
		}

		@Override
		public Mono<GovernanceTimelineRuntimeSummaryResponse> summary() {
			return Mono.just(new GovernanceTimelineRuntimeSummaryResponse(
					Instant.parse("2026-05-15T00:00:00Z"),
					GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY,
					GovernanceTimelineHealthStatus.DEGRADED_CAPABLE,
					GovernanceTimelineResilienceMode.PARTIAL_DEGRADED,
					true,
					true,
					true,
					List.of("timeline:DEGRADED_CAPABLE"),
					"degraded"
			));
		}
	}
}
