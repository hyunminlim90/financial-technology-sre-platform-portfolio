package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;
import com.fintech.sre.agent.governance.timeline.DefaultGovernanceTimelineCursorCodec;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineActor;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineActorType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationRequest;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationResult;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineCursor;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineCursorCodec;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEvent;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEventMetadata;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEventType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineFilter;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineProjection;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineProjectionType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineQuery;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResource;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResourceType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineSeverity;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ProjectionBackedGovernanceTimelineAggregationIntegrationTest {

	private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
			"cursor",
			"eventId",
			"sourceId",
			"incidentId",
			"query",
			"exception",
			"exceptionMessage",
			"summary",
			"metadata"
	);

	private final Clock fixedClock = Clock.fixed(
			Instant.parse("2026-05-17T04:05:06Z"),
			ZoneOffset.UTC
	);
	private final GovernanceTimelineCursorCodec cursorCodec =
			new DefaultGovernanceTimelineCursorCodec(
					new ObjectMapper().findAndRegisterModules()
			);

	@Test
	void shouldWriteProjectionAndQueryThroughProjectionBackedAggregation() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store =
				new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);
		ProjectionBackedGovernanceTimelineAggregationService aggregationService =
				aggregationService(store, registry);

		GovernanceTimelineProjectionWriteResult writeResult = writer.write(
				projection("event-1", "source-1", "2026-05-17T00:00:00Z")
		).block();
		GovernanceTimelineAggregationResult result = aggregationService.aggregate(
				request(10)
		).block();

		assertThat(writeResult).isNotNull();
		assertThat(writeResult.status())
				.isEqualTo(GovernanceTimelineProjectionWriteStatus.APPENDED);
		assertThat(result).isNotNull();
		assertThat(result.degraded()).isFalse();
		assertThat(result.reason()).isEqualTo("none");
		assertThat(result.page().items()).hasSize(1);
		assertThat(result.page().items().get(0).recordId()).isEqualTo("event-1");
		assertThat(result.page().items().get(0).type()).isEqualTo("RECOMMENDATION_CREATED");
		assertThat(result.page().items().get(0).status()).isEqualTo("INFO");
		assertThat(result.page().page().degraded()).isFalse();
		assertThat(result.page().page().nextCursor()).isNotNull();
		assertThat(result.page().page().previousCursor()).isNotNull();
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
	}

	@Test
	void shouldKeepStableOrdering() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store =
				new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);
		ProjectionBackedGovernanceTimelineAggregationService aggregationService =
				aggregationService(store, registry);

		writer.write(projection("rec-1", "source-1", "2026-05-17T00:00:00Z")).block();
		writer.write(projection("rec-2", "source-2", "2026-05-17T00:00:00Z")).block();
		writer.write(projection("rec-3", "source-3", "2026-05-17T01:00:00Z")).block();

		GovernanceTimelineAggregationResult result = aggregationService.aggregate(
				request(10)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("rec-3", "rec-2", "rec-1");
	}

	@Test
	void shouldNotDuplicateQueryResultAfterDuplicateWrite() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store =
				new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);
		ProjectionBackedGovernanceTimelineAggregationService aggregationService =
				aggregationService(store, registry);
		GovernanceTimelineProjection projection =
				projection("event-1", "source-1", "2026-05-17T00:00:00Z");

		GovernanceTimelineProjectionWriteResult first = writer.write(projection).block();
		GovernanceTimelineProjectionWriteResult second = writer.write(projection).block();
		GovernanceTimelineAggregationResult result = aggregationService.aggregate(
				request(10)
		).block();

		assertThat(first).isNotNull();
		assertThat(first.status())
				.isEqualTo(GovernanceTimelineProjectionWriteStatus.APPENDED);
		assertThat(second).isNotNull();
		assertThat(second.status())
				.isEqualTo(
						GovernanceTimelineProjectionWriteStatus.DUPLICATE_SKIPPED
				);
		assertThat(result).isNotNull();
		assertThat(result.page().items()).hasSize(1);
		assertThat(result.page().items().get(0).recordId()).isEqualTo("event-1");
	}

	@Test
	void shouldReturnEmptyPageForEmptyProjectionStore() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ProjectionBackedGovernanceTimelineAggregationService aggregationService =
				aggregationService(new InMemoryGovernanceTimelineProjectionStore(), registry);

		GovernanceTimelineAggregationResult result = aggregationService.aggregate(
				request(10)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.degraded()).isFalse();
		assertThat(result.page().items()).isEmpty();
		assertThat(result.page().page().limit()).isEqualTo(10);
		assertThat(result.page().page().direction())
				.isEqualTo(GovernanceCursorDirection.NEXT);
		assertThat(result.page().page().ordering())
				.isEqualTo("occurredAt DESC, eventId DESC");
		assertThat(result.page().page().degraded()).isFalse();
		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
						.tag("result", "empty")
						.tag("direction", "NEXT")
						.counter()
						.count()
		).isEqualTo(1.0);
	}

	@Test
	void shouldApplyEventTypeAndInclusiveTimeRangeFilters() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store =
				new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);
		ProjectionBackedGovernanceTimelineAggregationService aggregationService =
				aggregationService(store, registry);

		writer.write(projection(
				"event-1",
				"source-1",
				"2026-05-17T00:00:00Z",
				GovernanceTimelineEventType.RECOMMENDATION_CREATED
		)).block();
		writer.write(projection(
				"event-2",
				"source-2",
				"2026-05-17T01:00:00Z",
				GovernanceTimelineEventType.INCIDENT_TRANSITIONED
		)).block();
		writer.write(projection(
				"event-3",
				"source-3",
				"2026-05-17T02:00:00Z",
				GovernanceTimelineEventType.INCIDENT_TRANSITIONED
		)).block();

		GovernanceTimelineAggregationResult result = aggregationService.aggregate(
				request(
						10,
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								Instant.parse("2026-05-17T01:00:00Z"),
								Instant.parse("2026-05-17T02:00:00Z"),
								List.of(GovernanceTimelineEventType.INCIDENT_TRANSITIONED),
								false
						)
				)
		).block();

		assertThat(result).isNotNull();
		assertThat(result.page().items())
				.extracting(item -> item.recordId())
				.containsExactly("event-3", "event-2");
		assertThat(result.page().page().limit()).isEqualTo(10);
		assertThat(result.page().page().degraded()).isFalse();
	}

	@Test
	void shouldApplyLimitAfterFilteringInProjectionBackedAggregation() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store =
				new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);
		ProjectionBackedGovernanceTimelineAggregationService aggregationService =
				aggregationService(store, registry);

		writer.write(projection(
				"event-1",
				"source-1",
				"2026-05-17T00:00:00Z",
				GovernanceTimelineEventType.INCIDENT_TRANSITIONED
		)).block();
		writer.write(projection(
				"event-2",
				"source-2",
				"2026-05-17T01:00:00Z",
				GovernanceTimelineEventType.INCIDENT_TRANSITIONED
		)).block();
		writer.write(projection(
				"event-3",
				"source-3",
				"2026-05-17T02:00:00Z",
				GovernanceTimelineEventType.INCIDENT_TRANSITIONED
		)).block();

		GovernanceTimelineAggregationResult result = aggregationService.aggregate(
				request(
						2,
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
		assertThat(result.page().page().limit()).isEqualTo(2);
		assertThat(result.page().page().hasNext()).isTrue();
		assertThat(result.page().page().hasPrevious()).isFalse();
	}

	@Test
	void shouldApplyCursorPaginationInProjectionBackedAggregation() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store =
				new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);
		ProjectionBackedGovernanceTimelineAggregationService aggregationService =
				aggregationService(store, registry);

		writer.write(projection("event-4", "source-4", "2026-05-17T03:00:00Z")).block();
		writer.write(projection("event-3", "source-3", "2026-05-17T02:00:00Z")).block();
		writer.write(projection("event-2", "source-2", "2026-05-17T01:00:00Z")).block();
		writer.write(projection("event-1", "source-1", "2026-05-17T00:00:00Z")).block();

		GovernanceTimelineAggregationResult result = aggregationService.aggregate(
				request(
						cursor("2026-05-17T02:00:00Z", "RECOMMENDATION_CREATED", "event-3"),
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
	void shouldKeepMetricsLowCardinalityEndToEnd() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store =
				new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);
		ProjectionBackedGovernanceTimelineAggregationService aggregationService =
				aggregationService(store, registry);

		GovernanceTimelineProjection projection =
				projection("event-1", "source-1", "2026-05-17T00:00:00Z");

		writer.write(projection).block();
		writer.write(projection).block();
		aggregationService.aggregate(request(10)).block();
		assertThatThrownBy(() -> aggregationService.aggregate(
				request("invalid", 10, GovernanceCursorDirection.NEXT, defaultFilter())
		).block()).isInstanceOf(Exception.class);

		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "appended")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "duplicate_skipped")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
						.tag("result", "success")
						.tag("direction", "NEXT")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
						.tag("result", "invalid_cursor")
						.tag("direction", "NEXT")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.PAGE_SIZE)
						.tag("direction", "NEXT")
						.summary()
						.count()
		).isEqualTo(1L);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	private DefaultGovernanceTimelineProjectionWriter writer(
			SimpleMeterRegistry registry,
			InMemoryGovernanceTimelineProjectionStore store
	) {
		return new DefaultGovernanceTimelineProjectionWriter(
				new DefaultGovernanceTimelineProjectionRecordMapper(fixedClock),
				store,
				new GovernanceTimelineProjectionMetricsRecorder(registry)
		);
	}

	private ProjectionBackedGovernanceTimelineAggregationService aggregationService(
			InMemoryGovernanceTimelineProjectionStore store,
			SimpleMeterRegistry registry
	) {
		return new ProjectionBackedGovernanceTimelineAggregationService(
				new DefaultGovernanceTimelineProjectionQueryAdapter(
						store,
						cursorCodec,
						new GovernanceTimelineProjectionQueryMetricsRecorder(registry)
				)
		);
	}

	private GovernanceTimelineAggregationRequest request(int limit) {
		return request(null, limit, GovernanceCursorDirection.NEXT, defaultFilter());
	}

	private GovernanceTimelineAggregationRequest request(
			int limit,
			GovernanceTimelineFilter filter
	) {
		return request(null, limit, GovernanceCursorDirection.NEXT, filter);
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

	private GovernanceTimelineProjection projection(
			String eventId,
			String sourceId,
			String occurredAt
	) {
		return projection(
				eventId,
				sourceId,
				occurredAt,
				GovernanceTimelineEventType.RECOMMENDATION_CREATED
		);
	}

	private GovernanceTimelineProjection projection(
			String eventId,
			String sourceId,
			String occurredAt,
			GovernanceTimelineEventType eventType
	) {
		return new GovernanceTimelineProjection(
				GovernanceTimelineProjectionType.RECOMMENDATION_RECORD,
				sourceId,
				"incident-1",
				new GovernanceTimelineEvent(
						eventId,
						eventType,
						Instant.parse(occurredAt),
						"Recommendation created",
						"Sanitized summary",
						GovernanceTimelineSeverity.INFO,
						new GovernanceTimelineActor(
								GovernanceTimelineActorType.AI,
								"actor-1",
								"AI"
						),
						new GovernanceTimelineResource(
								GovernanceTimelineResourceType.RECOMMENDATION,
								"rec-1",
								"Recommendation"
						),
						new GovernanceTimelineEventMetadata(
								Map.of("category", "recommendation")
						),
						false
				)
		);
	}

	private void assertNoForbiddenTagKeys(Iterable<Meter> meters) {
		for (Meter meter : meters) {
			assertThat(meter.getId().getTags())
					.noneMatch(tag -> FORBIDDEN_TAG_KEYS.contains(tag.getKey()));
		}
	}
}
