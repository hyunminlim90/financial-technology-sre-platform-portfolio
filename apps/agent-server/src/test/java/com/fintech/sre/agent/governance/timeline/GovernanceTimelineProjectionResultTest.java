package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionResultTest {

	@Test
	void shouldCreateSuccessProjectionResultWithDefaults() {
		GovernanceTimelineProjection projection = projection();

		GovernanceTimelineProjectionResult result =
				GovernanceTimelineProjectionResult.success(List.of(projection));

		assertThat(result.projections()).containsExactly(projection);
		assertThat(result.degraded()).isFalse();
		assertThat(result.failedSources()).isEmpty();
		assertThat(result.reason()).isEqualTo("none");
	}

	@Test
	void shouldCreateDegradedProjectionResult() {
		GovernanceTimelineProjection projection = projection();

		GovernanceTimelineProjectionResult result =
				GovernanceTimelineProjectionResult.degraded(
						List.of(projection),
						List.of("APPROVAL_RECORD"),
						"projection_failed"
				);

		assertThat(result.projections()).containsExactly(projection);
		assertThat(result.degraded()).isTrue();
		assertThat(result.failedSources()).containsExactly("APPROVAL_RECORD");
		assertThat(result.reason()).isEqualTo("projection_failed");
	}

	private GovernanceTimelineProjection projection() {
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				"RECOMMENDATION_RECORD:rec-1",
				GovernanceTimelineEventType.RECOMMENDATION_CREATED,
				Instant.parse("2026-05-13T00:00:00Z"),
				"Recommendation created",
				"Payment recommendation generated",
				GovernanceTimelineSeverity.INFO,
				new GovernanceTimelineActor(
						GovernanceTimelineActorType.AI,
						"ai-agent",
						"AI Agent"
				),
				new GovernanceTimelineResource(
						GovernanceTimelineResourceType.RECOMMENDATION,
						"rec-1",
						"Recommendation rec-1"
				),
				new GovernanceTimelineEventMetadata(
						Map.of("service", "payment-api")
				),
				false
		);

		return new GovernanceTimelineProjection(
				GovernanceTimelineProjectionType.RECOMMENDATION_RECORD,
				"rec-1",
				"incident-1",
				event
		);
	}
}
