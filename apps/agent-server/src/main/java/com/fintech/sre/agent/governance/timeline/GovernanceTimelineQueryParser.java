package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

@Component
public class GovernanceTimelineQueryParser {

	private static final String INVALID_QUERY_MESSAGE = "Invalid timeline query.";

	public GovernanceTimelineAggregationRequest parse(
			String cursor,
			String direction,
			Integer limit,
			String from,
			String to,
			List<String> eventTypes,
			Boolean includeDegraded
	) {
		Instant parsedFrom = parseInstant(from);
		Instant parsedTo = parseInstant(to);
		validateTimeRange(parsedFrom, parsedTo, from, to);

		return new GovernanceTimelineAggregationRequest(
				new GovernanceTimelineQuery(
						cursor,
						limit,
						parseDirection(direction),
						new GovernanceTimelineFilter(
								null,
								null,
								null,
								null,
								parsedFrom,
								parsedTo,
								parseEventTypes(eventTypes),
								Boolean.TRUE.equals(includeDegraded)
						)
				),
				List.of()
		);
	}

	private GovernanceCursorDirection parseDirection(String direction) {
		if (direction == null || direction.isBlank()) {
			return GovernanceCursorDirection.NEXT;
		}

		try {
			return GovernanceCursorDirection.valueOf(
					direction.trim().toUpperCase(Locale.ROOT)
			);
		} catch (IllegalArgumentException ex) {
			throw invalidQuery();
		}
	}

	private List<GovernanceTimelineEventType> parseEventTypes(List<String> eventTypes) {
		if (eventTypes == null || eventTypes.isEmpty()) {
			return List.of();
		}

		try {
			return eventTypes.stream()
					.filter(value -> value != null && !value.isBlank())
					.map(value -> GovernanceTimelineEventType.valueOf(
							value.trim().toUpperCase(Locale.ROOT)
					))
					.toList();
		} catch (IllegalArgumentException ex) {
			throw invalidQuery();
		}
	}

	private Instant parseInstant(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return Instant.parse(value.trim());
		} catch (DateTimeParseException ex) {
			throw invalidQuery();
		}
	}

	private void validateTimeRange(
			Instant from,
			Instant to,
			String rawFrom,
			String rawTo
	) {
		boolean hasFrom = rawFrom != null && !rawFrom.isBlank();
		boolean hasTo = rawTo != null && !rawTo.isBlank();

		if (hasFrom != hasTo) {
			throw invalidQuery();
		}
		if (from != null && to != null && from.isAfter(to)) {
			throw invalidQuery();
		}
	}

	private IllegalArgumentException invalidQuery() {
		return new IllegalArgumentException(INVALID_QUERY_MESSAGE);
	}
}
