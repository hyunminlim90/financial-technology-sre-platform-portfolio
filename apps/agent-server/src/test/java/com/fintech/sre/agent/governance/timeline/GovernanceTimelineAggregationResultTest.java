package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.detail.GovernanceDetailTimelineItem;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

class GovernanceTimelineAggregationResultTest {

	@Test
	void shouldUseAllSourcesWhenRequestSourcesAreMissing() {
		GovernanceTimelineAggregationRequest request =
				new GovernanceTimelineAggregationRequest(
						new GovernanceTimelineQuery(null, null, null, null),
						List.of()
				);

		assertThat(request.safeSources())
				.containsExactly(GovernanceTimelineAggregationSource.values());
	}

	@Test
	void shouldCreateSuccessAggregationResultWithDefaults() {
		GovernanceTimelineAggregationResult result =
				GovernanceTimelineAggregationResult.success(page());

		assertThat(result.page()).isEqualTo(page());
		assertThat(result.degraded()).isFalse();
		assertThat(result.failedSources()).isEmpty();
		assertThat(result.reason()).isEqualTo("none");
	}

	@Test
	void shouldCreateDegradedAggregationResult() {
		GovernanceTimelineAggregationResult result =
				GovernanceTimelineAggregationResult.degraded(
						page(),
						List.of("APPROVAL"),
						"timeline_aggregation_degraded"
				);

		assertThat(result.page()).isEqualTo(page());
		assertThat(result.degraded()).isTrue();
		assertThat(result.failedSources()).containsExactly("APPROVAL");
		assertThat(result.reason()).isEqualTo("timeline_aggregation_degraded");
	}

	private GovernanceTimelinePageResponse page() {
		return new GovernanceTimelinePageResponse(
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
	}
}
