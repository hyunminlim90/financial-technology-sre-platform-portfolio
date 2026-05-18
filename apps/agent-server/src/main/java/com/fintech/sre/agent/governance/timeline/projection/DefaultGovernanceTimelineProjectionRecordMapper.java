package com.fintech.sre.agent.governance.timeline.projection;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineActor;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEvent;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineProjection;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResource;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResourceType;

public class DefaultGovernanceTimelineProjectionRecordMapper
		implements GovernanceTimelineProjectionRecordMapper {

	private final Clock clock;

	public DefaultGovernanceTimelineProjectionRecordMapper(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Override
	public GovernanceTimelineProjectionRecord map(
			GovernanceTimelineProjection projection
	) {
		Objects.requireNonNull(projection, "projection must not be null");

		GovernanceTimelineEvent event = Objects.requireNonNull(
				projection.event(),
				"projection.event must not be null"
		);

		return new GovernanceTimelineProjectionRecord(
				event.eventId(),
				eventType(event),
				event.occurredAt(),
				projection.sourceType() == null ? null : projection.sourceType().name(),
				projection.sourceId(),
				projection.incidentId() != null
						? projection.incidentId()
						: resourceId(event, GovernanceTimelineResourceType.INCIDENT),
				resourceId(event, GovernanceTimelineResourceType.RECOMMENDATION),
				resourceId(event, GovernanceTimelineResourceType.LEARNING),
				resourceId(event, GovernanceTimelineResourceType.KNOWLEDGE_UPDATE),
				severity(event),
				actorType(event.actor()),
				resourceType(event.resource()),
				event.title(),
				event.summary(),
				metadata(event),
				event.degraded(),
				Instant.now(clock)
		);
	}

	private String eventType(GovernanceTimelineEvent event) {
		return event.eventType() == null ? null : event.eventType().name();
	}

	private String severity(GovernanceTimelineEvent event) {
		return event.severity() == null ? null : event.severity().name();
	}

	private String actorType(GovernanceTimelineActor actor) {
		return actor == null || actor.type() == null ? null : actor.type().name();
	}

	private String resourceType(GovernanceTimelineResource resource) {
		return resource == null || resource.type() == null
				? null
				: resource.type().name();
	}

	private Map<String, Object> metadata(GovernanceTimelineEvent event) {
		if (event.metadata() == null || event.metadata().attributes() == null) {
			return Map.of();
		}

		return Map.copyOf(event.metadata().attributes());
	}

	private String resourceId(
			GovernanceTimelineEvent event,
			GovernanceTimelineResourceType type
	) {
		if (event.resource() == null || event.resource().type() != type) {
			return null;
		}
		return event.resource().id();
	}
}
