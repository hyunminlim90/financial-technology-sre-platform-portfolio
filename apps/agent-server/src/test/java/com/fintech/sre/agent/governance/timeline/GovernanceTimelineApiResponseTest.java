package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.detail.GovernanceDetailTimelineItem;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

class GovernanceTimelineApiResponseTest {

	@Test
	void shouldRetainResponseEnvelope() {
		GovernanceTimelinePageResponse page = new GovernanceTimelinePageResponse(
				List.of(new GovernanceDetailTimelineItem(
						Instant.parse("2026-05-13T00:00:00Z"),
						"RECOMMENDATION_CREATED",
						"rec-1",
						"ALLOW",
						"Recommendation rec-1",
						"payment-api / payment"
				)),
				new GovernanceTimelinePageMetadata(
						"next-cursor",
						"previous-cursor",
						true,
						false,
						50,
						GovernanceCursorDirection.NEXT,
						"occurredAt DESC, eventId DESC",
						false,
						List.of()
				)
		);
		GovernanceTimelineApiResponse response = new GovernanceTimelineApiResponse(
				Instant.parse("2026-05-13T00:00:01Z"),
				GovernanceTimelineApiStatus.DEGRADED,
				page,
				GovernanceTimelineDegradation.partial(
						GovernanceTimelineResilienceMode.PARTIAL_DEGRADED,
						List.of(new GovernanceTimelineComponentFailure(
								GovernanceTimelineAggregationSource.VERIFICATION,
								"component_query_timeout"
						)),
						"timeline_query_timeout"
				),
				List.of(new GovernanceTimelineApiError(
						"timeline_partial",
						"Timeline is available with partial degraded data."
				))
		);

		assertThat(GovernanceTimelineApiStatus.values()).containsExactly(
				GovernanceTimelineApiStatus.SUCCESS,
				GovernanceTimelineApiStatus.DEGRADED,
				GovernanceTimelineApiStatus.FAILURE
		);
		assertThat(response.status()).isEqualTo(GovernanceTimelineApiStatus.DEGRADED);
		assertThat(response.page()).isEqualTo(page);
		assertThat(response.degradation().degraded()).isTrue();
		assertThat(response.errors()).containsExactly(
				new GovernanceTimelineApiError(
						"timeline_partial",
						"Timeline is available with partial degraded data."
				)
		);
	}
}
