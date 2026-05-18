package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class DefaultGovernanceTimelineProjectionWriterIntegrationTest {

	private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
			"eventId",
			"sourceId",
			"incidentId",
			"recommendationRecordId",
			"learningCandidateId",
			"knowledgeUpdateApplicationId",
			"exception",
			"exceptionMessage",
			"summary",
			"metadata",
			"rawPayload"
	);

	private final Clock fixedClock = Clock.fixed(
			Instant.parse("2026-05-17T03:04:05Z"),
			ZoneOffset.UTC
	);

	@Test
	void shouldWriteProjectionAndStoreRecord() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store = new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);

		GovernanceTimelineProjectionWriteResult result = writer.write(
				projection("event-1", "source-1", "2026-05-17T00:00:00Z")
		).block();

		assertThat(result).isNotNull();
		assertThat(result.status())
				.isEqualTo(GovernanceTimelineProjectionWriteStatus.APPENDED);

		List<GovernanceTimelineProjectionRecord> records =
				store.findRecent(10).collectList().block();

		assertThat(records).hasSize(1);
		assertThat(records.get(0).eventId()).isEqualTo("event-1");
		assertThat(records.get(0).eventType()).isEqualTo("RECOMMENDATION_CREATED");
		assertThat(records.get(0).sourceType()).isEqualTo("RECOMMENDATION_RECORD");
		assertThat(records.get(0).sourceId()).isEqualTo("source-1");
		assertThat(records.get(0).metadata())
				.containsEntry("category", "recommendation");
	}

	@Test
	void shouldKeepDuplicateWriteIdempotent() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store = new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);

		GovernanceTimelineProjection projection =
				projection("event-1", "source-1", "2026-05-17T00:00:00Z");

		GovernanceTimelineProjectionWriteResult first = writer.write(projection).block();
		GovernanceTimelineProjectionWriteResult second = writer.write(projection).block();

		assertThat(first).isNotNull();
		assertThat(first.status())
				.isEqualTo(GovernanceTimelineProjectionWriteStatus.APPENDED);
		assertThat(second).isNotNull();
		assertThat(second.status())
				.isEqualTo(
						GovernanceTimelineProjectionWriteStatus.DUPLICATE_SKIPPED
				);
	}

	@Test
	void shouldKeepStableOrderingInStore() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store = new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);

		writer.write(projection("rec-1", "source-1", "2026-05-17T00:00:00Z"))
				.block();
		writer.write(projection("rec-2", "source-2", "2026-05-17T00:00:00Z"))
				.block();
		writer.write(projection("rec-3", "source-3", "2026-05-17T01:00:00Z"))
				.block();

		List<GovernanceTimelineProjectionRecord> records =
				store.findRecent(10).collectList().block();

		assertThat(records)
				.extracting(GovernanceTimelineProjectionRecord::eventId)
				.containsExactly("rec-3", "rec-2", "rec-1");
	}

	@Test
	void shouldRecordMetricsEndToEnd() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		InMemoryGovernanceTimelineProjectionStore store = new InMemoryGovernanceTimelineProjectionStore();
		DefaultGovernanceTimelineProjectionWriter writer = writer(registry, store);

		GovernanceTimelineProjection projection =
				projection("event-1", "source-1", "2026-05-17T00:00:00Z");

		writer.write(projection).block();
		writer.write(projection).block();

		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "appended")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "duplicate_skipped")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	private DefaultGovernanceTimelineProjectionWriter writer(
			SimpleMeterRegistry registry,
			InMemoryGovernanceTimelineProjectionStore store
	) {
		return new DefaultGovernanceTimelineProjectionWriter(
				new DefaultGovernanceTimelineProjectionRecordMapper(fixedClock),
				store,
				new GovernanceTimelineProjectionMetricsRecorder(registry)
		);
	}

	private GovernanceTimelineProjection projection(
			String eventId,
			String sourceId,
			String occurredAt
	) {
		return new GovernanceTimelineProjection(
				GovernanceTimelineProjectionType.RECOMMENDATION_RECORD,
				sourceId,
				"incident-1",
				new GovernanceTimelineEvent(
						eventId,
						GovernanceTimelineEventType.RECOMMENDATION_CREATED,
						Instant.parse(occurredAt),
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
	}

	private void assertNoForbiddenTagKeys(Iterable<Meter> meters) {
		for (Meter meter : meters) {
			assertThat(meter.getId().getTags())
					.noneMatch(tag -> FORBIDDEN_TAG_KEYS.contains(tag.getKey()));
		}
	}
}
