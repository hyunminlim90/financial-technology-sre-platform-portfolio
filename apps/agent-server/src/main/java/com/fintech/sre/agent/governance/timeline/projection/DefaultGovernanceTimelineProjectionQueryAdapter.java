package com.fintech.sre.agent.governance.timeline.projection;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fintech.sre.agent.governance.detail.GovernanceDetailTimelineItem;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineActor;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineActorType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationRequest;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationResult;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineCursor;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineCursorCodec;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineCursorDecodeException;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEvent;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEventMetadata;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEventType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineFilter;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelinePageMetadata;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelinePageResponse;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResource;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResourceType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineSeverity;

import reactor.core.publisher.Mono;

public class DefaultGovernanceTimelineProjectionQueryAdapter
		implements GovernanceTimelineProjectionQueryAdapter {

	private static final String ORDERING = "occurredAt DESC, eventId DESC";
	private static final int MAX_FETCH_LIMIT = 500;

	private final GovernanceTimelineProjectionStore store;
	private final GovernanceTimelineCursorCodec cursorCodec;
	private final GovernanceTimelineProjectionQueryMetricsRecorder metricsRecorder;

	public DefaultGovernanceTimelineProjectionQueryAdapter(
			GovernanceTimelineProjectionStore store,
			GovernanceTimelineCursorCodec cursorCodec,
			GovernanceTimelineProjectionQueryMetricsRecorder metricsRecorder
	) {
		this.store = Objects.requireNonNull(store, "store must not be null");
		this.cursorCodec = Objects.requireNonNull(
				cursorCodec,
				"cursorCodec must not be null"
		);
		this.metricsRecorder = Objects.requireNonNull(
				metricsRecorder,
				"metricsRecorder must not be null"
		);
	}

	@Override
	public Mono<GovernanceTimelineAggregationResult> query(
			GovernanceTimelineAggregationRequest request
	) {
		Objects.requireNonNull(request, "request must not be null");
		Objects.requireNonNull(request.query(), "request.query must not be null");

		int limit = request.query().safeLimit();
		int fetchLimit = Math.min(Math.max(limit * 4, limit), MAX_FETCH_LIMIT);
		GovernanceCursorDirection direction = request.query().safeDirection();
		GovernanceTimelineCursor cursor;
		try {
			cursor = decodeCursor(request.query().cursor());
		} catch (GovernanceTimelineCursorDecodeException ex) {
			metricsRecorder.query("invalid_cursor", direction);
			throw ex;
		}

		return store.findRecent(fetchLimit)
				.map(this::toEvent)
				.filter(event -> matchesFilter(event, request.query().filter()))
				.filter(event -> withinCursorBoundary(event, cursor, direction))
				.collectList()
				.map(events -> toResult(events, limit, direction, cursor))
				.map(page -> GovernanceTimelineAggregationResult.success(
						new GovernanceTimelinePageResponse(
								page.items(),
								page.metadata()
						)
				))
				.doOnNext(result -> recordSuccessMetrics(result, direction))
				.doOnError(error -> metricsRecorder.failure(direction));
	}

	private void recordSuccessMetrics(
			GovernanceTimelineAggregationResult result,
			GovernanceCursorDirection direction
	) {
		int size = result == null || result.page() == null || result.page().items() == null
				? 0
				: result.page().items().size();
		metricsRecorder.query(size == 0 ? "empty" : "success", direction);
		metricsRecorder.pageSize(direction, size);
	}

	private PageProjection toResult(
			List<GovernanceTimelineEvent> events,
			int limit,
			GovernanceCursorDirection direction,
			GovernanceTimelineCursor cursor
	) {
		boolean hasMore = events.size() > limit;
		List<GovernanceTimelineEvent> pageEvents = events.stream()
				.limit(limit)
				.toList();
		List<GovernanceDetailTimelineItem> items = pageEvents.stream()
				.map(this::toTimelineItem)
				.toList();
		boolean hasNext;
		boolean hasPrevious;
		if (cursor == null) {
			hasNext = hasMore;
			hasPrevious = false;
		} else if (direction == GovernanceCursorDirection.PREVIOUS) {
			hasNext = true;
			hasPrevious = hasMore;
		} else {
			hasNext = hasMore;
			hasPrevious = true;
		}
		String previousCursor = pageEvents.isEmpty() ? null : cursorOf(pageEvents.get(0));
		String nextCursor = pageEvents.isEmpty()
				? null
				: cursorOf(pageEvents.get(pageEvents.size() - 1));

		return new PageProjection(
				items,
				new GovernanceTimelinePageMetadata(
						nextCursor,
						previousCursor,
						hasNext,
						hasPrevious,
						limit,
						direction,
						ORDERING,
						false,
						List.of()
				)
		);
	}

	private GovernanceTimelineCursor decodeCursor(String encodedCursor) {
		if (encodedCursor == null || encodedCursor.isBlank()) {
			return null;
		}
		return cursorCodec.decode(encodedCursor);
	}

	private boolean matchesFilter(
			GovernanceTimelineEvent event,
			GovernanceTimelineFilter filter
	) {
		if (filter == null) {
			return true;
		}
		return matchesEventTypes(event, filter.eventTypes())
				&& matchesInclusiveTimeRange(event.occurredAt(), filter.from(), filter.to());
	}

	private boolean matchesEventTypes(
			GovernanceTimelineEvent event,
			List<GovernanceTimelineEventType> eventTypes
	) {
		if (eventTypes == null || eventTypes.isEmpty()) {
			return true;
		}
		return event.eventType() != null && eventTypes.contains(event.eventType());
	}

	private boolean matchesInclusiveTimeRange(
			Instant occurredAt,
			Instant from,
			Instant to
	) {
		if (occurredAt == null) {
			return false;
		}
		if (from != null && occurredAt.isBefore(from)) {
			return false;
		}
		if (to != null && occurredAt.isAfter(to)) {
			return false;
		}
		return true;
	}

	private boolean withinCursorBoundary(
			GovernanceTimelineEvent event,
			GovernanceTimelineCursor cursor,
			GovernanceCursorDirection direction
	) {
		if (cursor == null) {
			return true;
		}

		int occurredAtComparison = compareOccurredAt(event.occurredAt(), cursor.occurredAt());
		int eventIdComparison = compareEventId(event.eventId(), cursor.eventId());

		if (direction == GovernanceCursorDirection.PREVIOUS) {
			return occurredAtComparison > 0
					|| (occurredAtComparison == 0 && eventIdComparison > 0);
		}
		return occurredAtComparison < 0
				|| (occurredAtComparison == 0 && eventIdComparison < 0);
	}

	private int compareOccurredAt(Instant left, Instant right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return -1;
		}
		if (right == null) {
			return 1;
		}
		return left.compareTo(right);
	}

	private int compareEventId(String left, String right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return -1;
		}
		if (right == null) {
			return 1;
		}
		return left.compareTo(right);
	}

	GovernanceTimelineEvent toEvent(GovernanceTimelineProjectionRecord record) {
		Objects.requireNonNull(record, "record must not be null");

		return new GovernanceTimelineEvent(
				record.eventId(),
				GovernanceTimelineEventType.valueOf(record.eventType()),
				record.occurredAt(),
				record.title(),
				record.summary(),
				GovernanceTimelineSeverity.valueOf(record.severity()),
				new GovernanceTimelineActor(
						GovernanceTimelineActorType.valueOf(record.actorType()),
						null,
						null
				),
				new GovernanceTimelineResource(
						GovernanceTimelineResourceType.valueOf(record.resourceType()),
						resourceId(record),
						null
				),
				new GovernanceTimelineEventMetadata(stringMetadata(record.metadata())),
				record.degraded()
		);
	}

	private GovernanceDetailTimelineItem toTimelineItem(
			GovernanceTimelineEvent event
	) {
		return new GovernanceDetailTimelineItem(
				event.occurredAt(),
				event.eventType().name(),
				event.eventId(),
				event.severity().name(),
				event.title(),
				event.summary()
		);
	}

	private String cursorOf(GovernanceTimelineEvent event) {
		return cursorCodec.encode(new GovernanceTimelineCursor(
				event.occurredAt(),
				event.eventType().name(),
				event.eventId()
		));
	}

	private Map<String, String> stringMetadata(Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return Map.of();
		}

		return metadata.entrySet().stream()
				.collect(
						java.util.stream.Collectors.toUnmodifiableMap(
								Map.Entry::getKey,
								entry -> String.valueOf(entry.getValue())
						)
				);
	}

	private String resourceId(GovernanceTimelineProjectionRecord record) {
		if (record.incidentId() != null) {
			return record.incidentId();
		}
		if (record.recommendationRecordId() != null) {
			return record.recommendationRecordId();
		}
		if (record.learningCandidateId() != null) {
			return record.learningCandidateId();
		}
		if (record.knowledgeUpdateApplicationId() != null) {
			return record.knowledgeUpdateApplicationId();
		}
		return null;
	}

	private record PageProjection(
			List<GovernanceDetailTimelineItem> items,
			GovernanceTimelinePageMetadata metadata
	) {
	}
}
