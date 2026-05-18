package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

class GovernanceTimelineQueryTest {

	@Test
	void shouldClampLimitAndDefaultDirection() {
		GovernanceTimelineQuery defaultQuery = new GovernanceTimelineQuery(
				"cursor",
				null,
				null,
				null
		);
		GovernanceTimelineQuery maxQuery = new GovernanceTimelineQuery(
				"cursor",
				500,
				GovernanceCursorDirection.PREVIOUS,
				null
		);

		assertThat(defaultQuery.safeLimit()).isEqualTo(50);
		assertThat(defaultQuery.safeDirection()).isEqualTo(
				GovernanceCursorDirection.NEXT
		);
		assertThat(maxQuery.safeLimit()).isEqualTo(200);
		assertThat(maxQuery.safeDirection()).isEqualTo(
				GovernanceCursorDirection.PREVIOUS
		);
	}

	@Test
	void shouldRetainFilterValues() {
		GovernanceTimelineFilter filter = new GovernanceTimelineFilter(
				"incident-1",
				"rec-1",
				"candidate-1",
				"update-1",
				Instant.parse("2026-05-12T00:00:00Z"),
				Instant.parse("2026-05-13T00:00:00Z"),
				List.of(
						GovernanceTimelineEventType.RECOMMENDATION_CREATED,
						GovernanceTimelineEventType.KNOWLEDGE_UPDATED
				),
				true
		);
		GovernanceTimelineQuery query = new GovernanceTimelineQuery(
				"opaque-cursor",
				25,
				GovernanceCursorDirection.NEXT,
				filter
		);

		assertThat(query.filter()).isEqualTo(filter);
		assertThat(query.filter().incidentId()).isEqualTo("incident-1");
		assertThat(query.filter().recommendationRecordId()).isEqualTo("rec-1");
		assertThat(query.filter().learningCandidateId()).isEqualTo("candidate-1");
		assertThat(query.filter().knowledgeUpdateApplicationId()).isEqualTo("update-1");
		assertThat(query.filter().eventTypes()).containsExactly(
				GovernanceTimelineEventType.RECOMMENDATION_CREATED,
				GovernanceTimelineEventType.KNOWLEDGE_UPDATED
		);
		assertThat(query.filter().includeDegraded()).isTrue();
	}
}
