package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;
import com.fintech.sre.agent.governance.timeline.DefaultGovernanceTimelineCursorCodec;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationRequest;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationResult;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineCursor;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineCursorCodec;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineCursorDecodeException;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEvent;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEventType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineFilter;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelinePageResponse;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineQuery;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class DefaultGovernanceTimelineProjectionQueryAdapterTest {

	private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
			"cursor",
			"eventId",
			"sourceId",
			"incidentId",
			"recommendationRecordId",
			"learningCandidateId",
			"knowledgeUpdateApplicationId",
			"query",
			"exception",
			"exceptionMessage",
			"summary",
			"metadata",
			"rawPayload"
	);

	private final GovernanceTimelineCursorCodec cursorCodec =
			new DefaultGovernanceTimelineCursorCodec(
					new ObjectMapper().findAndRegisterModules()
			);

	@Test
	void shouldQueryStoreWithSafeLimitAndConvertRecords() {
		CapturingStore store = new CapturingStore(List.of(
				record("event-1", "RECOMMENDATION_CREATED", "2026-05-17T00:00:00Z")
		));
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		GovernanceTimelineAggregationResult result = adapter(store, registry).query(
				request(10, GovernanceCursorDirection.NEXT)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.degraded()).isFalse();
		assertThat(result.failedSources()).isEmpty();
		assertThat(result.reason()).isEqualTo("none");

		GovernanceTimelinePageResponse page = result.page();
		assertThat(page.items()).hasSize(1);
		assertThat(page.items().get(0).occurredAt())
				.isEqualTo(Instant.parse("2026-05-17T00:00:00Z"));
		assertThat(page.items().get(0).type()).isEqualTo("RECOMMENDATION_CREATED");
		assertThat(page.items().get(0).recordId()).isEqualTo("event-1");
		assertThat(page.items().get(0).status()).isEqualTo("INFO");
		assertThat(page.items().get(0).title()).isEqualTo("title-event-1");
		assertThat(page.items().get(0).summary()).isEqualTo("summary-event-1");
		assertThat(page.page().nextCursor()).isNotNull();
		assertThat(page.page().previousCursor()).isNotNull();
		assertThat(page.page().hasNext()).isFalse();
		assertThat(page.page().hasPrevious()).isFalse();
		assertThat(page.page().limit()).isEqualTo(10);
		assertThat(page.page().direction()).isEqualTo(GovernanceCursorDirection.NEXT);
		assertThat(page.page().ordering()).isEqualTo("occurredAt DESC, eventId DESC");
		assertThat(page.page().degraded()).isFalse();
		assertThat(page.page().failedComponents()).isEmpty();
		assertThat(store.requestedLimit).isEqualTo(40);
		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
						.tag("result", "success")
						.tag("direction", "NEXT")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.PAGE_SIZE)
						.tag("direction", "NEXT")
						.summary()
						.totalAmount()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldReturnEmptyPageForEmptyStore() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		GovernanceTimelineAggregationResult result = adapter(
				new CapturingStore(List.of()),
				registry
		).query(
				request(5, GovernanceCursorDirection.PREVIOUS)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items()).isEmpty();
		assertThat(result.page().page().limit()).isEqualTo(5);
		assertThat(result.page().page().direction())
				.isEqualTo(GovernanceCursorDirection.PREVIOUS);
		assertThat(result.page().page().degraded()).isFalse();
		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
						.tag("result", "empty")
						.tag("direction", "PREVIOUS")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.PAGE_SIZE)
						.tag("direction", "PREVIOUS")
						.summary()
						.totalAmount()
		).isEqualTo(0.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldFilterByEventType() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-1", "RECOMMENDATION_CREATED", "2026-05-17T00:00:00Z"),
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z")
		))).query(
				request(
						10,
						GovernanceCursorDirection.NEXT,
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								null,
								null,
								List.of(GovernanceTimelineEventType.INCIDENT_TRANSITIONED),
								false
						)
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.type())
				.containsExactly("INCIDENT_TRANSITIONED");
	}

	@Test
	void shouldReturnAllWhenEventTypeFilterIsEmpty() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-1", "RECOMMENDATION_CREATED", "2026-05-17T00:00:00Z"),
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z")
		))).query(
				request(
						10,
						GovernanceCursorDirection.NEXT,
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								null,
								null,
								List.of(),
								false
						)
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items()).hasSize(2);
	}

	@Test
	void shouldApplyInclusiveTimeRange() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-3", "INCIDENT_TRANSITIONED", "2026-05-17T02:00:00Z"),
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-1", "RECOMMENDATION_CREATED", "2026-05-17T00:00:00Z")
		))).query(
				request(
						10,
						GovernanceCursorDirection.NEXT,
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								Instant.parse("2026-05-17T01:00:00Z"),
								Instant.parse("2026-05-17T02:00:00Z"),
								List.of(),
								false
						)
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-3", "event-2");
	}

	@Test
	void shouldExcludeOutsideTimeRange() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-1", "RECOMMENDATION_CREATED", "2026-05-17T00:00:00Z"),
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-3", "INCIDENT_TRANSITIONED", "2026-05-17T02:00:00Z")
		))).query(
				request(
						10,
						GovernanceCursorDirection.NEXT,
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								Instant.parse("2026-05-17T00:30:00Z"),
								Instant.parse("2026-05-17T01:30:00Z"),
								List.of(),
								false
						)
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-2");
	}

	@Test
	void shouldApplyLimitAfterFiltering() {
		CapturingStore store = new CapturingStore(List.of(
				record("event-3", "INCIDENT_TRANSITIONED", "2026-05-17T02:00:00Z"),
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-1", "INCIDENT_TRANSITIONED", "2026-05-17T00:00:00Z")
		));

		GovernanceTimelineAggregationResult result = adapter(store).query(
				request(
						2,
						GovernanceCursorDirection.NEXT,
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								null,
								null,
								List.of(GovernanceTimelineEventType.INCIDENT_TRANSITIONED),
								false
						)
				)
		).block();

		assertThat(store.requestedLimit).isEqualTo(8);
		assertThat(result).isNotNull();
		assertThat(result.page().items()).hasSize(2);
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-3", "event-2");
	}

	@Test
	void shouldReturnEmptyPageWhenFiltersRemoveEverything() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-1", "RECOMMENDATION_CREATED", "2026-05-17T00:00:00Z")
		))).query(
				request(
						10,
						GovernanceCursorDirection.NEXT,
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								null,
								null,
								List.of(GovernanceTimelineEventType.KNOWLEDGE_UPDATED),
								false
						)
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items()).isEmpty();
		assertThat(result.page().page().degraded()).isFalse();
	}

	@Test
	void shouldRestoreMetadataWhenConvertingRecordToEvent() {
		GovernanceTimelineEvent event = adapter(new CapturingStore(List.of())).toEvent(
				new GovernanceTimelineProjectionRecord(
						"event-1",
						"KNOWLEDGE_UPDATED",
						Instant.parse("2026-05-17T00:00:00Z"),
						"KNOWLEDGE_UPDATE_APPLICATION",
						"source-1",
						null,
						null,
						null,
						"knowledge-1",
						"WARNING",
						"HUMAN",
						"KNOWLEDGE_UPDATE",
						"title",
						"summary",
						Map.of("state", "APPLIED", "attempt", 2),
						true,
						Instant.parse("2026-05-17T00:00:01Z")
				)
		);

		assertThat(event.eventId()).isEqualTo("event-1");
		assertThat(event.eventType().name()).isEqualTo("KNOWLEDGE_UPDATED");
		assertThat(event.severity().name()).isEqualTo("WARNING");
		assertThat(event.actor().type().name()).isEqualTo("HUMAN");
		assertThat(event.resource().type().name()).isEqualTo("KNOWLEDGE_UPDATE");
		assertThat(event.resource().id()).isEqualTo("knowledge-1");
		assertThat(event.metadata().attributes())
				.containsEntry("state", "APPLIED")
				.containsEntry("attempt", "2");
		assertThat(event.degraded()).isTrue();
	}

	@Test
	void shouldRejectNullRequest() {
		assertThatThrownBy(() -> adapter(new CapturingStore(List.of())).query(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("request must not be null");
	}

	@Test
	void shouldReturnOlderEventsForNextCursor() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-4", "INCIDENT_TRANSITIONED", "2026-05-17T03:00:00Z"),
				record("event-3", "INCIDENT_TRANSITIONED", "2026-05-17T02:00:00Z"),
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-1", "INCIDENT_TRANSITIONED", "2026-05-17T00:00:00Z")
		))).query(
				request(
						cursor("2026-05-17T02:00:00Z", "INCIDENT_TRANSITIONED", "event-3"),
						2,
						GovernanceCursorDirection.NEXT,
						defaultFilter()
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-2", "event-1");
		assertThat(result.page().page().hasNext()).isFalse();
		assertThat(result.page().page().hasPrevious()).isTrue();
		assertThat(result.page().page().nextCursor()).isNotNull();
		assertThat(result.page().page().previousCursor()).isNotNull();
	}

	@Test
	void shouldReturnNewerEventsForPreviousCursor() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-4", "INCIDENT_TRANSITIONED", "2026-05-17T03:00:00Z"),
				record("event-3", "INCIDENT_TRANSITIONED", "2026-05-17T02:00:00Z"),
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-1", "INCIDENT_TRANSITIONED", "2026-05-17T00:00:00Z")
		))).query(
				request(
						cursor("2026-05-17T01:00:00Z", "INCIDENT_TRANSITIONED", "event-2"),
						2,
						GovernanceCursorDirection.PREVIOUS,
						defaultFilter()
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-4", "event-3");
		assertThat(result.page().page().hasNext()).isTrue();
		assertThat(result.page().page().hasPrevious()).isFalse();
	}

	@Test
	void shouldUseEventIdTieBreakerForNextCursor() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-c", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-b", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-a", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z")
		))).query(
				request(
						cursor("2026-05-17T01:00:00Z", "INCIDENT_TRANSITIONED", "event-b"),
						10,
						GovernanceCursorDirection.NEXT,
						defaultFilter()
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-a");
	}

	@Test
	void shouldUseEventIdTieBreakerForPreviousCursor() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-c", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-b", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-a", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z")
		))).query(
				request(
						cursor("2026-05-17T01:00:00Z", "INCIDENT_TRANSITIONED", "event-b"),
						10,
						GovernanceCursorDirection.PREVIOUS,
						defaultFilter()
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-c");
	}

	@Test
	void shouldFailSafelyForInvalidCursor() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		assertThatThrownBy(() -> adapter(
				new CapturingStore(List.of(record(
						"event-1",
						"INCIDENT_TRANSITIONED",
						"2026-05-17T01:00:00Z"
				))),
				registry
		).query(
				request("invalid", 10, GovernanceCursorDirection.NEXT, defaultFilter())
		).block())
				.isInstanceOf(GovernanceTimelineCursorDecodeException.class)
				.hasMessage("Invalid timeline cursor.");

		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
						.tag("result", "invalid_cursor")
						.tag("direction", "NEXT")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldReturnEmptyPageWhenCursorRemovesEverything() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-1", "INCIDENT_TRANSITIONED", "2026-05-17T00:00:00Z")
		))).query(
				request(
						cursor("2026-05-17T00:00:00Z", "INCIDENT_TRANSITIONED", "event-1"),
						10,
						GovernanceCursorDirection.NEXT,
						defaultFilter()
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items()).isEmpty();
		assertThat(result.page().page().nextCursor()).isNull();
		assertThat(result.page().page().previousCursor()).isNull();
	}

	@Test
	void shouldCombineEventTypeFilterWithCursor() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-4", "RECOMMENDATION_CREATED", "2026-05-17T03:00:00Z"),
				record("event-3", "INCIDENT_TRANSITIONED", "2026-05-17T02:00:00Z"),
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-1", "RECOMMENDATION_CREATED", "2026-05-17T00:00:00Z")
		))).query(
				request(
						cursor("2026-05-17T03:00:00Z", "RECOMMENDATION_CREATED", "event-4"),
						10,
						GovernanceCursorDirection.NEXT,
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								null,
								null,
								List.of(GovernanceTimelineEventType.INCIDENT_TRANSITIONED),
								false
						)
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-3", "event-2");
	}

	@Test
	void shouldCombineTimeRangeFilterWithCursor() {
		GovernanceTimelineAggregationResult result = adapter(new CapturingStore(List.of(
				record("event-4", "INCIDENT_TRANSITIONED", "2026-05-17T03:00:00Z"),
				record("event-3", "INCIDENT_TRANSITIONED", "2026-05-17T02:00:00Z"),
				record("event-2", "INCIDENT_TRANSITIONED", "2026-05-17T01:00:00Z"),
				record("event-1", "INCIDENT_TRANSITIONED", "2026-05-17T00:00:00Z")
		))).query(
				request(
						cursor("2026-05-17T03:00:00Z", "INCIDENT_TRANSITIONED", "event-4"),
						10,
						GovernanceCursorDirection.NEXT,
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								Instant.parse("2026-05-17T01:00:00Z"),
								Instant.parse("2026-05-17T02:00:00Z"),
								List.of(),
								false
						)
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-3", "event-2");
	}

	@Test
	void shouldRecordFailureMetricWhenStoreFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		assertThatThrownBy(() -> adapter(new FailingStore(), registry).query(
				request(10, GovernanceCursorDirection.NEXT)
		).block())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("projection store failed");

		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_FAILURE_TOTAL)
						.tag("result", "failure")
						.tag("direction", "NEXT")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	private DefaultGovernanceTimelineProjectionQueryAdapter adapter(
			GovernanceTimelineProjectionStore store
	) {
		return adapter(store, new SimpleMeterRegistry());
	}

	private DefaultGovernanceTimelineProjectionQueryAdapter adapter(
			GovernanceTimelineProjectionStore store,
			SimpleMeterRegistry registry
	) {
		return new DefaultGovernanceTimelineProjectionQueryAdapter(
				store,
				cursorCodec,
				new GovernanceTimelineProjectionQueryMetricsRecorder(registry)
		);
	}

	private GovernanceTimelineAggregationRequest request(
			int limit,
			GovernanceCursorDirection direction
	) {
		return request(null, limit, direction, defaultFilter());
	}

	private GovernanceTimelineAggregationRequest request(
			int limit,
			GovernanceCursorDirection direction,
			GovernanceTimelineFilter filter
	) {
		return request(null, limit, direction, filter);
	}

	private GovernanceTimelineAggregationRequest request(
			String cursor,
			int limit,
			GovernanceCursorDirection direction,
			GovernanceTimelineFilter filter
	) {
		return new GovernanceTimelineAggregationRequest(
				new GovernanceTimelineQuery(
						cursor,
						limit,
						direction,
						filter
				),
				List.of()
		);
	}

	private GovernanceTimelineFilter defaultFilter() {
		return new GovernanceTimelineFilter(
				null,
				null,
				null,
				null,
				null,
				null,
				List.of(),
				false
		);
	}

	private String cursor(
			String occurredAt,
			String eventType,
			String eventId
	) {
		return cursorCodec.encode(new GovernanceTimelineCursor(
				Instant.parse(occurredAt),
				eventType,
				eventId
		));
	}

	private GovernanceTimelineProjectionRecord record(
			String eventId,
			String eventType,
			String occurredAt
	) {
		return new GovernanceTimelineProjectionRecord(
				eventId,
				eventType,
				Instant.parse(occurredAt),
				"RECOMMENDATION_RECORD",
				eventId + "-source",
				"incident-1",
				"rec-1",
				null,
				null,
				"INFO",
				"AI",
				"RECOMMENDATION",
				"title-" + eventId,
				"summary-" + eventId,
				Map.of("category", "recommendation"),
				false,
				Instant.parse("2026-05-17T00:00:01Z")
		);
	}

	private static final class CapturingStore
			implements GovernanceTimelineProjectionStore {

		private final List<GovernanceTimelineProjectionRecord> records;
		private int requestedLimit;

		private CapturingStore(List<GovernanceTimelineProjectionRecord> records) {
			this.records = List.copyOf(records);
		}

		@Override
		public Mono<GovernanceTimelineProjectionWriteResult> append(
				GovernanceTimelineProjectionRecord record
		) {
			return Mono.just(
					GovernanceTimelineProjectionWriteResult.appended(record.eventId())
			);
		}

		@Override
		public Flux<GovernanceTimelineProjectionRecord> findRecent(int limit) {
			this.requestedLimit = limit;
			return Flux.fromIterable(records);
		}
	}

	private static final class FailingStore
			implements GovernanceTimelineProjectionStore {

		@Override
		public Mono<GovernanceTimelineProjectionWriteResult> append(
				GovernanceTimelineProjectionRecord record
		) {
			return Mono.just(
					GovernanceTimelineProjectionWriteResult.appended(
							record == null ? "unknown" : record.eventId()
					)
			);
		}

		@Override
		public Flux<GovernanceTimelineProjectionRecord> findRecent(int limit) {
			return Flux.error(new IllegalStateException("projection store failed"));
		}
	}

	private void assertNoForbiddenTagKeys(Iterable<Meter> meters) {
		for (Meter meter : meters) {
			assertThat(meter.getId().getTags())
					.noneMatch(tag -> FORBIDDEN_TAG_KEYS.contains(tag.getKey()));
		}
	}
}
