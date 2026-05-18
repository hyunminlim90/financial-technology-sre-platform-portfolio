package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.detail.GovernanceDetailTimelineItem;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

class GovernanceTimelinePageResponseTest {

	@Test
	void shouldRetainTimelineItemsAndMetadata() {
		GovernanceDetailTimelineItem item = new GovernanceDetailTimelineItem(
				Instant.parse("2026-05-12T00:00:00Z"),
				"RECOMMENDATION_CREATED",
				"rec-1",
				"ALLOW",
				"Recommendation rec-1",
				"payment-api / payment"
		);
		GovernanceTimelinePageMetadata metadata = new GovernanceTimelinePageMetadata(
				"next-cursor",
				"previous-cursor",
				true,
				false,
				50,
				GovernanceCursorDirection.NEXT,
				"occurredAt DESC, eventId DESC",
				true,
				List.of("approvals")
		);
		GovernanceTimelinePageResponse response =
				new GovernanceTimelinePageResponse(
						List.of(item),
						metadata
				);

		assertThat(response.items()).containsExactly(item);
		assertThat(response.page().nextCursor()).isEqualTo("next-cursor");
		assertThat(response.page().previousCursor()).isEqualTo("previous-cursor");
		assertThat(response.page().direction()).isEqualTo(
				GovernanceCursorDirection.NEXT
		);
		assertThat(response.page().ordering()).isEqualTo(
				"occurredAt DESC, eventId DESC"
		);
		assertThat(response.page().degraded()).isTrue();
		assertThat(response.page().failedComponents()).containsExactly("approvals");
	}
}
