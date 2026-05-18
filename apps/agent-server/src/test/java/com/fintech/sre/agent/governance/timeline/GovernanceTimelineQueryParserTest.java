package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

class GovernanceTimelineQueryParserTest {

	private final GovernanceTimelineQueryParser parser =
			new GovernanceTimelineQueryParser();

	@Test
	void shouldParseDefaultQuery() {
		GovernanceTimelineAggregationRequest request = parser.parse(
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);

		assertThat(request.query().safeLimit()).isEqualTo(50);
		assertThat(request.query().safeDirection()).isEqualTo(
				GovernanceCursorDirection.NEXT
		);
		assertThat(request.query().filter().eventTypes()).isEmpty();
		assertThat(request.query().filter().includeDegraded()).isFalse();
	}

	@Test
	void shouldClampLimit() {
		GovernanceTimelineAggregationRequest request = parser.parse(
				null,
				null,
				999,
				null,
				null,
				null,
				null
		);

		assertThat(request.query().safeLimit()).isEqualTo(200);
	}

	@Test
	void shouldParseDirection() {
		GovernanceTimelineAggregationRequest request = parser.parse(
				null,
				"previous",
				10,
				null,
				null,
				null,
				null
		);

		assertThat(request.query().safeDirection()).isEqualTo(
				GovernanceCursorDirection.PREVIOUS
		);
	}

	@Test
	void shouldParseRepeatedEventTypes() {
		GovernanceTimelineAggregationRequest request = parser.parse(
				null,
				null,
				10,
				null,
				null,
				List.of(
						"RECOMMENDATION_CREATED",
						"VERIFICATION_RECORDED"
				),
				true
		);

		assertThat(request.query().filter().eventTypes()).containsExactly(
				GovernanceTimelineEventType.RECOMMENDATION_CREATED,
				GovernanceTimelineEventType.VERIFICATION_RECORDED
		);
		assertThat(request.query().filter().includeDegraded()).isTrue();
	}

	@Test
	void shouldParseFromAndTo() {
		GovernanceTimelineAggregationRequest request = parser.parse(
				null,
				null,
				10,
				"2026-05-14T00:00:00Z",
				"2026-05-14T01:00:00Z",
				List.of(),
				false
		);

		assertThat(request.query().filter().from()).isEqualTo(
				Instant.parse("2026-05-14T00:00:00Z")
		);
		assertThat(request.query().filter().to()).isEqualTo(
				Instant.parse("2026-05-14T01:00:00Z")
		);
	}

	@Test
	void shouldRejectInvalidTimeRange() {
		assertThatThrownBy(() -> parser.parse(
				null,
				null,
				10,
				"2026-05-14T02:00:00Z",
				"2026-05-14T01:00:00Z",
				List.of(),
				false
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid timeline query.");
	}

	@Test
	void shouldRejectHalfBoundedTimeRange() {
		assertThatThrownBy(() -> parser.parse(
				null,
				null,
				10,
				"2026-05-14T00:00:00Z",
				null,
				List.of(),
				false
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid timeline query.");
	}
}
