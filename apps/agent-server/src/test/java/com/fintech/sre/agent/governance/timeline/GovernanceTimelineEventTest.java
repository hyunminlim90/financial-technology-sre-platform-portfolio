package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GovernanceTimelineEventTest {

	@Test
	void shouldRetainNormalizedTimelineEventShape() {
		GovernanceTimelineActor actor = new GovernanceTimelineActor(
				GovernanceTimelineActorType.HUMAN,
				"operator-1",
				"Operator One"
		);
		GovernanceTimelineResource resource = new GovernanceTimelineResource(
				GovernanceTimelineResourceType.RECOMMENDATION,
				"rec-1",
				"Recommendation rec-1"
		);
		GovernanceTimelineEventMetadata metadata =
				new GovernanceTimelineEventMetadata(
						Map.of("service", "payment-api", "domain", "payment")
				);
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				"event-1",
				GovernanceTimelineEventType.RECOMMENDATION_CREATED,
				Instant.parse("2026-05-13T00:00:00Z"),
				"Recommendation created",
				"Payment recommendation generated",
				GovernanceTimelineSeverity.INFO,
				actor,
				resource,
				metadata,
				false
		);

		assertThat(event.eventId()).isEqualTo("event-1");
		assertThat(event.eventType()).isEqualTo(
				GovernanceTimelineEventType.RECOMMENDATION_CREATED
		);
		assertThat(event.occurredAt()).isEqualTo(
				Instant.parse("2026-05-13T00:00:00Z")
		);
		assertThat(event.severity()).isEqualTo(GovernanceTimelineSeverity.INFO);
		assertThat(event.actor()).isEqualTo(actor);
		assertThat(event.resource()).isEqualTo(resource);
		assertThat(event.metadata().attributes())
				.containsEntry("service", "payment-api")
				.containsEntry("domain", "payment");
		assertThat(event.degraded()).isFalse();
	}
}
