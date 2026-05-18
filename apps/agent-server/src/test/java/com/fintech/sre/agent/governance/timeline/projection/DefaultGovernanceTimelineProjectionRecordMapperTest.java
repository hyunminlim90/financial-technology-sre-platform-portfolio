package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineActor;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineActorType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEvent;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEventMetadata;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineEventType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineProjection;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineProjectionType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResource;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResourceType;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineSeverity;

class DefaultGovernanceTimelineProjectionRecordMapperTest {

	private final Clock fixedClock = Clock.fixed(
			Instant.parse("2026-05-17T01:02:03Z"),
			ZoneOffset.UTC
	);

	private final DefaultGovernanceTimelineProjectionRecordMapper mapper =
			new DefaultGovernanceTimelineProjectionRecordMapper(fixedClock);

	@Test
	void shouldMapProjectionToRecord() {
		GovernanceTimelineProjection projection = new GovernanceTimelineProjection(
				GovernanceTimelineProjectionType.RECOMMENDATION_RECORD,
				"source-1",
				"incident-1",
				new GovernanceTimelineEvent(
						"event-1",
						GovernanceTimelineEventType.RECOMMENDATION_CREATED,
						Instant.parse("2026-05-17T00:00:00Z"),
						"Recommendation created",
						"Sanitized summary",
						GovernanceTimelineSeverity.INFO,
						new GovernanceTimelineActor(
								GovernanceTimelineActorType.AI,
								"actor-1",
								"AI"
						),
						new GovernanceTimelineResource(
								GovernanceTimelineResourceType.RECOMMENDATION,
								"rec-1",
								"Recommendation"
						),
						new GovernanceTimelineEventMetadata(
								Map.of("category", "recommendation")
						),
						false
				)
		);

		GovernanceTimelineProjectionRecord record = mapper.map(projection);

		assertThat(record.eventId()).isEqualTo("event-1");
		assertThat(record.eventType()).isEqualTo("RECOMMENDATION_CREATED");
		assertThat(record.occurredAt()).isEqualTo(Instant.parse("2026-05-17T00:00:00Z"));
		assertThat(record.sourceType()).isEqualTo("RECOMMENDATION_RECORD");
		assertThat(record.sourceId()).isEqualTo("source-1");
		assertThat(record.incidentId()).isEqualTo("incident-1");
		assertThat(record.recommendationRecordId()).isEqualTo("rec-1");
		assertThat(record.learningCandidateId()).isNull();
		assertThat(record.knowledgeUpdateApplicationId()).isNull();
		assertThat(record.severity()).isEqualTo("INFO");
		assertThat(record.actorType()).isEqualTo("AI");
		assertThat(record.resourceType()).isEqualTo("RECOMMENDATION");
		assertThat(record.title()).isEqualTo("Recommendation created");
		assertThat(record.summary()).isEqualTo("Sanitized summary");
		assertThat(record.metadata()).containsEntry("category", "recommendation");
		assertThat(record.degraded()).isFalse();
		assertThat(record.createdAt()).isEqualTo(Instant.parse("2026-05-17T01:02:03Z"));
	}

	@Test
	void shouldDefensivelyCopyMetadata() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("state", "OPEN");

		GovernanceTimelineProjection projection = new GovernanceTimelineProjection(
				GovernanceTimelineProjectionType.INCIDENT_LIFECYCLE,
				"source-1",
				"incident-1",
				new GovernanceTimelineEvent(
						"event-1",
						GovernanceTimelineEventType.INCIDENT_TRANSITIONED,
						Instant.parse("2026-05-17T00:00:00Z"),
						"title",
						"summary",
						GovernanceTimelineSeverity.INFO,
						new GovernanceTimelineActor(
								GovernanceTimelineActorType.SYSTEM,
								"actor-1",
								"System"
						),
						new GovernanceTimelineResource(
								GovernanceTimelineResourceType.INCIDENT,
								"incident-1",
								"Incident"
						),
						new GovernanceTimelineEventMetadata(metadata),
						false
				)
		);

		GovernanceTimelineProjectionRecord record = mapper.map(projection);
		metadata.put("state", "RESOLVED");

		assertThat(record.metadata()).containsEntry("state", "OPEN");
		assertThatThrownBy(() -> record.metadata().put("state", "MUTATED"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void shouldUseResourceIdsByResourceType() {
		GovernanceTimelineProjection learningProjection = new GovernanceTimelineProjection(
				GovernanceTimelineProjectionType.LEARNING_CANDIDATE,
				"source-1",
				null,
				new GovernanceTimelineEvent(
						"event-learning",
						GovernanceTimelineEventType.LEARNING_CANDIDATE_CREATED,
						Instant.parse("2026-05-17T00:00:00Z"),
						"title",
						"summary",
						GovernanceTimelineSeverity.INFO,
						new GovernanceTimelineActor(
								GovernanceTimelineActorType.SYSTEM,
								"actor-1",
								"System"
						),
						new GovernanceTimelineResource(
								GovernanceTimelineResourceType.LEARNING,
								"learning-1",
								"Learning"
						),
						new GovernanceTimelineEventMetadata(Map.of()),
						false
				)
		);
		GovernanceTimelineProjection knowledgeProjection =
				new GovernanceTimelineProjection(
						GovernanceTimelineProjectionType.KNOWLEDGE_UPDATE_APPLICATION,
						"source-2",
						null,
						new GovernanceTimelineEvent(
								"event-knowledge",
								GovernanceTimelineEventType.KNOWLEDGE_UPDATED,
								Instant.parse("2026-05-17T00:00:00Z"),
								"title",
								"summary",
								GovernanceTimelineSeverity.INFO,
								new GovernanceTimelineActor(
										GovernanceTimelineActorType.HUMAN,
										"actor-2",
										"Human"
								),
								new GovernanceTimelineResource(
										GovernanceTimelineResourceType.KNOWLEDGE_UPDATE,
										"knowledge-1",
										"Knowledge Update"
								),
								new GovernanceTimelineEventMetadata(Map.of()),
								false
						)
				);

		GovernanceTimelineProjectionRecord learningRecord =
				mapper.map(learningProjection);
		GovernanceTimelineProjectionRecord knowledgeRecord =
				mapper.map(knowledgeProjection);

		assertThat(learningRecord.learningCandidateId()).isEqualTo("learning-1");
		assertThat(learningRecord.recommendationRecordId()).isNull();
		assertThat(knowledgeRecord.knowledgeUpdateApplicationId())
				.isEqualTo("knowledge-1");
		assertThat(knowledgeRecord.incidentId()).isNull();
	}

	@Test
	void shouldRejectNullProjection() {
		assertThatThrownBy(() -> mapper.map(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("projection must not be null");
	}
}
