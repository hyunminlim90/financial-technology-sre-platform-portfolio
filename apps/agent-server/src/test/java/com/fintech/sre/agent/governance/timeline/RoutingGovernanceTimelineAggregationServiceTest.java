package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.detail.GovernanceDetailTimelineItem;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

import reactor.core.publisher.Mono;

class RoutingGovernanceTimelineAggregationServiceTest {

	@Test
	void shouldRouteToRuntimeFanOutService() {
		GovernanceTimelineAggregationModeProperties properties =
				new GovernanceTimelineAggregationModeProperties();
		properties.setMode(GovernanceTimelineAggregationMode.RUNTIME_FAN_OUT);

		CapturingAggregationService runtimeService = new CapturingAggregationService(
				result("runtime")
		);
		CapturingAggregationService projectionService =
				new CapturingAggregationService(result("projection"));
		RoutingGovernanceTimelineAggregationService service =
				new RoutingGovernanceTimelineAggregationService(
						properties,
						runtimeService,
						projectionService
				);
		GovernanceTimelineAggregationRequest request = request();

		GovernanceTimelineAggregationResult result = service.aggregate(request).block();

		assertThat(runtimeService.captured).isSameAs(request);
		assertThat(projectionService.captured).isNull();
		assertThat(result).isEqualTo(runtimeService.result);
	}

	@Test
	void shouldRouteToProjectionBackedService() {
		GovernanceTimelineAggregationModeProperties properties =
				new GovernanceTimelineAggregationModeProperties();
		properties.setMode(GovernanceTimelineAggregationMode.PROJECTION_BACKED);

		CapturingAggregationService runtimeService = new CapturingAggregationService(
				result("runtime")
		);
		CapturingAggregationService projectionService =
				new CapturingAggregationService(result("projection"));
		RoutingGovernanceTimelineAggregationService service =
				new RoutingGovernanceTimelineAggregationService(
						properties,
						runtimeService,
						projectionService
				);
		GovernanceTimelineAggregationRequest request = request();

		GovernanceTimelineAggregationResult result = service.aggregate(request).block();

		assertThat(runtimeService.captured).isNull();
		assertThat(projectionService.captured).isSameAs(request);
		assertThat(result).isEqualTo(projectionService.result);
	}

	@Test
	void shouldRejectNullRequest() {
		RoutingGovernanceTimelineAggregationService service =
				new RoutingGovernanceTimelineAggregationService(
						new GovernanceTimelineAggregationModeProperties(),
						new CapturingAggregationService(result("runtime")),
						new CapturingAggregationService(result("projection"))
				);

		assertThatThrownBy(() -> service.aggregate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("request must not be null");
	}

	@Test
	void shouldRejectNullProperties() {
		assertThatThrownBy(
				() -> new RoutingGovernanceTimelineAggregationService(
						null,
						new CapturingAggregationService(result("runtime")),
						new CapturingAggregationService(result("projection"))
				)
		)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("properties must not be null");
	}

	@Test
	void shouldRejectNullRuntimeAggregationService() {
		assertThatThrownBy(
				() -> new RoutingGovernanceTimelineAggregationService(
						new GovernanceTimelineAggregationModeProperties(),
						null,
						new CapturingAggregationService(result("projection"))
				)
		)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("runtimeAggregationService must not be null");
	}

	@Test
	void shouldRejectNullProjectionBackedAggregationService() {
		assertThatThrownBy(
				() -> new RoutingGovernanceTimelineAggregationService(
						new GovernanceTimelineAggregationModeProperties(),
						new CapturingAggregationService(result("runtime")),
						null
				)
		)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("projectionBackedAggregationService must not be null");
	}

	@Test
	void shouldPropagateDelegateFailure() {
		GovernanceTimelineAggregationModeProperties properties =
				new GovernanceTimelineAggregationModeProperties();
		properties.setMode(GovernanceTimelineAggregationMode.PROJECTION_BACKED);

		RoutingGovernanceTimelineAggregationService service =
				new RoutingGovernanceTimelineAggregationService(
						properties,
						new CapturingAggregationService(result("runtime")),
						request -> Mono.error(new IllegalStateException("delegate failure"))
				);

		assertThatThrownBy(() -> service.aggregate(request()).block())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("delegate failure");
	}

	private GovernanceTimelineAggregationRequest request() {
		return new GovernanceTimelineAggregationRequest(
				new GovernanceTimelineQuery(
						null,
						10,
						GovernanceCursorDirection.NEXT,
						null
				),
				List.of()
		);
	}

	private GovernanceTimelineAggregationResult result(String recordId) {
		return GovernanceTimelineAggregationResult.success(
				new GovernanceTimelinePageResponse(
						List.of(
								new GovernanceDetailTimelineItem(
										java.time.Instant.parse("2026-05-17T00:00:00Z"),
										"RECOMMENDATION_CREATED",
										recordId,
										"INFO",
										"title",
										"summary"
								)
						),
						new GovernanceTimelinePageMetadata(
								null,
								null,
								false,
								false,
								10,
								GovernanceCursorDirection.NEXT,
								"occurredAt DESC, eventId DESC",
								false,
								List.of()
						)
				)
		);
	}

	private static final class CapturingAggregationService
			implements GovernanceTimelineAggregationService {

		private final GovernanceTimelineAggregationResult result;
		private GovernanceTimelineAggregationRequest captured;

		private CapturingAggregationService(GovernanceTimelineAggregationResult result) {
			this.result = result;
		}

		@Override
		public Mono<GovernanceTimelineAggregationResult> aggregate(
				GovernanceTimelineAggregationRequest request
		) {
			this.captured = request;
			return Mono.just(result);
		}
	}
}
