package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fintech.sre.agent.governance.detail.GovernanceDetailTimelineItem;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

class GovernanceTimelineControllerTest {

	private WebTestClient webTestClient;
	private StubAggregationService aggregationService;
	private SimpleMeterRegistry meterRegistry;

	@BeforeEach
	void setUp() {
		aggregationService = new StubAggregationService();
		meterRegistry = new SimpleMeterRegistry();
		webTestClient = WebTestClient.bindToController(
				new GovernanceTimelineController(
						aggregationService,
						new GovernanceTimelineQueryParser(),
						new GovernanceTimelineMetricsRecorder(
								new com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder(
										meterRegistry
								),
								meterRegistry
						)
				)
		).configureClient().build();
	}

	@Test
	void shouldReturnSuccessResponse() {
		aggregationService.result = Mono.just(successResult());

		webTestClient.get()
				.uri("/internal/governance/timeline?limit=10")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("SUCCESS")
				.jsonPath("$.page.items[0].recordId").isEqualTo("rec-1")
				.jsonPath("$.degradation.degraded").isEqualTo(false)
				.jsonPath("$.errors.length()").isEqualTo(0);

		assertThat(aggregationService.lastRequest.query().safeLimit()).isEqualTo(10);
		assertThat(meterRegistry.find(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "success")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnDegradedResponse() {
		aggregationService.result = Mono.just(degradedResult());

		webTestClient.get()
				.uri("/internal/governance/timeline")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("DEGRADED")
				.jsonPath("$.degradation.degraded").isEqualTo(true)
				.jsonPath("$.degradation.failedComponents[0].source")
				.isEqualTo("VERIFICATION");

		assertThat(meterRegistry.find(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "degraded")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldParseRepeatedEventTypesAndDirection() {
		aggregationService.result = Mono.just(successResult());

		webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path("/internal/governance/timeline")
						.queryParam("direction", "PREVIOUS")
						.queryParam("eventType", "RECOMMENDATION_CREATED")
						.queryParam("eventType", "VERIFICATION_RECORDED")
						.build())
				.exchange()
				.expectStatus().isOk();

		assertThat(aggregationService.lastRequest.query().safeDirection()).isEqualTo(
				GovernanceCursorDirection.PREVIOUS
		);
		assertThat(aggregationService.lastRequest.query().filter().eventTypes())
				.containsExactly(
						GovernanceTimelineEventType.RECOMMENDATION_CREATED,
						GovernanceTimelineEventType.VERIFICATION_RECORDED
				);
	}

	@Test
	void shouldReturnBadRequestForInvalidTimeRange() {
		webTestClient.get()
				.uri("/internal/governance/timeline?from=2026-05-14T02:00:00Z&to=2026-05-14T01:00:00Z")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.status").isEqualTo("FAILURE")
				.jsonPath("$.errors[0].code").isEqualTo("INVALID_TIMELINE_QUERY");

		assertThat(meterRegistry.find(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "invalid_query")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnBadRequestForHalfBoundedTimeRange() {
		webTestClient.get()
				.uri("/internal/governance/timeline?from=2026-05-14T02:00:00Z")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.status").isEqualTo("FAILURE")
				.jsonPath("$.page").doesNotExist()
				.jsonPath("$.degradation.degraded").isEqualTo(false)
				.jsonPath("$.errors[0].code").isEqualTo("INVALID_TIMELINE_QUERY")
				.jsonPath("$.errors[0].message").isEqualTo("Invalid timeline query.");
	}

	@Test
	void shouldReturnBadRequestForToWithoutFrom() {
		webTestClient.get()
				.uri("/internal/governance/timeline?to=2026-05-14T02:00:00Z")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.status").isEqualTo("FAILURE")
				.jsonPath("$.page").doesNotExist()
				.jsonPath("$.degradation.degraded").isEqualTo(false)
				.jsonPath("$.errors[0].code").isEqualTo("INVALID_TIMELINE_QUERY")
				.jsonPath("$.errors[0].message").isEqualTo("Invalid timeline query.");
	}

	@Test
	void shouldReturnBadRequestForInvalidDirection() {
		webTestClient.get()
				.uri("/internal/governance/timeline?direction=INVALID")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.status").isEqualTo("FAILURE")
				.jsonPath("$.page").doesNotExist()
				.jsonPath("$.degradation.degraded").isEqualTo(false)
				.jsonPath("$.errors[0].code").isEqualTo("INVALID_TIMELINE_QUERY")
				.jsonPath("$.errors[0].message").isEqualTo("Invalid timeline query.");
	}

	@Test
	void shouldReturnBadRequestForInvalidEventType() {
		webTestClient.get()
				.uri("/internal/governance/timeline?eventType=INVALID_EVENT")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.status").isEqualTo("FAILURE")
				.jsonPath("$.page").doesNotExist()
				.jsonPath("$.degradation.degraded").isEqualTo(false)
				.jsonPath("$.errors[0].code").isEqualTo("INVALID_TIMELINE_QUERY")
				.jsonPath("$.errors[0].message").isEqualTo("Invalid timeline query.");
	}

	@Test
	void shouldReturnBadRequestForInvalidCursor() {
		aggregationService.result = Mono.error(new GovernanceTimelineCursorDecodeException());

		webTestClient.get()
				.uri("/internal/governance/timeline?cursor=%%%25invalid")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.status").isEqualTo("FAILURE")
				.jsonPath("$.page").doesNotExist()
				.jsonPath("$.degradation.degraded").isEqualTo(false)
				.jsonPath("$.errors[0].code").isEqualTo("INVALID_TIMELINE_CURSOR")
				.jsonPath("$.errors[0].message").isEqualTo("Invalid timeline cursor.");

		assertThat(meterRegistry.find(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "invalid_cursor")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnSafeInternalServerErrorForUnexpectedFailure() {
		aggregationService.result = Mono.error(new RuntimeException("sensitive DB detail"));

		webTestClient.get()
				.uri("/internal/governance/timeline")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().is5xxServerError()
				.expectBody()
				.jsonPath("$.status").isEqualTo("FAILURE")
				.jsonPath("$.page").doesNotExist()
				.jsonPath("$.degradation.degraded").isEqualTo(false)
				.jsonPath("$.errors[0].code").isEqualTo("TIMELINE_QUERY_FAILED")
				.jsonPath("$.errors[0].message").isEqualTo("Timeline query failed.");

		assertThat(meterRegistry.find(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "failure")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRejectPostForTimelinePath() {
		webTestClient.post()
				.uri("/internal/governance/timeline")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().is4xxClientError();
	}

	@Test
	void shouldRejectPutForTimelinePath() {
		webTestClient.put()
				.uri("/internal/governance/timeline")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().is4xxClientError();
	}

	@Test
	void shouldRejectPatchForTimelinePath() {
		webTestClient.patch()
				.uri("/internal/governance/timeline")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().is4xxClientError();
	}

	@Test
	void shouldRejectDeleteForTimelinePath() {
		webTestClient.delete()
				.uri("/internal/governance/timeline")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().is4xxClientError();
	}

	@Test
	void shouldRecordEmptyMetric() {
		aggregationService.result = Mono.just(GovernanceTimelineAggregationResult.success(
				new GovernanceTimelinePageResponse(
						List.of(),
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
		));

		webTestClient.get()
				.uri("/internal/governance/timeline")
				.exchange()
				.expectStatus().isOk();

		assertThat(meterRegistry.find(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "empty")
				.counter()
				.count()).isEqualTo(1.0);
	}

	private GovernanceTimelineAggregationResult successResult() {
		GovernanceTimelinePageResponse page = new GovernanceTimelinePageResponse(
				List.of(new GovernanceDetailTimelineItem(
						Instant.parse("2026-05-14T00:00:00Z"),
						"RECOMMENDATION_CREATED",
						"rec-1",
						"CREATED",
						"Recommendation created",
						"Recommendation summary"
				)),
				new GovernanceTimelinePageMetadata(
						"next",
						"previous",
						false,
						false,
						10,
						GovernanceCursorDirection.NEXT,
						"occurredAt DESC, eventId DESC",
						false,
						List.of()
				)
		);
		return GovernanceTimelineAggregationResult.success(page);
	}

	private GovernanceTimelineAggregationResult degradedResult() {
		GovernanceTimelinePageResponse page = new GovernanceTimelinePageResponse(
				List.of(new GovernanceDetailTimelineItem(
						Instant.parse("2026-05-14T00:00:00Z"),
						"VERIFICATION_RECORDED",
						"verification-1",
						"VERIFIED",
						"Verification recorded",
						"Verification summary"
				)),
				new GovernanceTimelinePageMetadata(
						"next",
						"previous",
						false,
						false,
						10,
						GovernanceCursorDirection.NEXT,
						"occurredAt DESC, eventId DESC",
						true,
						List.of("VERIFICATION")
				)
		);
		return GovernanceTimelineAggregationResult.degraded(
				page,
				List.of("VERIFICATION"),
				"timeline_aggregation_degraded"
		);
	}

	private static final class StubAggregationService
			implements GovernanceTimelineAggregationService {

		private Mono<GovernanceTimelineAggregationResult> result =
				Mono.just(GovernanceTimelineAggregationResult.success(
						new GovernanceTimelinePageResponse(
								List.of(),
								new GovernanceTimelinePageMetadata(
										null,
										null,
										false,
										false,
										50,
										GovernanceCursorDirection.NEXT,
										"occurredAt DESC, eventId DESC",
										false,
										List.of()
								)
						)
				));
		private GovernanceTimelineAggregationRequest lastRequest;

		@Override
		public Mono<GovernanceTimelineAggregationResult> aggregate(
				GovernanceTimelineAggregationRequest request
		) {
			this.lastRequest = request;
			return result;
		}
	}
}
