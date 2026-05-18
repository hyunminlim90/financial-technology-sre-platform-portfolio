package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryGovernanceTimelineProjectionStoreTest {

	private final InMemoryGovernanceTimelineProjectionStore store =
			new InMemoryGovernanceTimelineProjectionStore();

	@Test
	void shouldAppendNewRecord() {
		GovernanceTimelineProjectionWriteResult result = store.append(
				record("event-1", "2026-05-17T00:00:00Z")
		).block();

		assertThat(result).isNotNull();
		assertThat(result.status())
				.isEqualTo(GovernanceTimelineProjectionWriteStatus.APPENDED);
		assertThat(result.eventId()).isEqualTo("event-1");
	}

	@Test
	void shouldSkipDuplicateEventId() {
		store.append(record("event-1", "2026-05-17T00:00:00Z")).block();

		GovernanceTimelineProjectionWriteResult result = store.append(
				record("event-1", "2026-05-17T00:01:00Z")
		).block();

		assertThat(result).isNotNull();
		assertThat(result.status())
				.isEqualTo(
						GovernanceTimelineProjectionWriteStatus.DUPLICATE_SKIPPED
				);
		assertThat(result.eventId()).isEqualTo("event-1");
	}

	@Test
	void shouldSortRecentByOccurredAtDescThenEventIdDesc() {
		store.append(record("event-1", "2026-05-17T00:00:00Z")).block();
		store.append(record("event-3", "2026-05-17T00:00:00Z")).block();
		store.append(record("event-2", "2026-05-17T01:00:00Z")).block();

		List<GovernanceTimelineProjectionRecord> records =
				store.findRecent(10).collectList().block();

		assertThat(records)
				.extracting(GovernanceTimelineProjectionRecord::eventId)
				.containsExactly("event-2", "event-3", "event-1");
	}

	@Test
	void shouldApplyLimit() {
		store.append(record("event-1", "2026-05-17T00:00:00Z")).block();
		store.append(record("event-2", "2026-05-17T01:00:00Z")).block();
		store.append(record("event-3", "2026-05-17T02:00:00Z")).block();

		List<GovernanceTimelineProjectionRecord> records =
				store.findRecent(2).collectList().block();

		assertThat(records).hasSize(2);
		assertThat(records)
				.extracting(GovernanceTimelineProjectionRecord::eventId)
				.containsExactly("event-3", "event-2");
	}

	@Test
	void shouldReturnEmptyWhenLimitIsZeroOrNegative() {
		assertThat(store.findRecent(0).collectList().block()).isEmpty();
		assertThat(store.findRecent(-1).collectList().block()).isEmpty();
	}

	@Test
	void shouldRejectNullRecord() {
		assertThatThrownBy(() -> store.append(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("record must not be null");
	}

	@Test
	void shouldKeepMetadataImmutableThroughStore() {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("state", "OPEN");
		GovernanceTimelineProjectionRecord record =
				new GovernanceTimelineProjectionRecord(
						"event-1",
						"INCIDENT_TRANSITIONED",
						Instant.parse("2026-05-17T00:00:00Z"),
						"INCIDENT_LIFECYCLE",
						"source-1",
						null,
						null,
						null,
						null,
						"INFO",
						"SYSTEM",
						"INCIDENT",
						"title",
						"summary",
						metadata,
						false,
						Instant.parse("2026-05-17T00:00:01Z")
				);

		store.append(record).block();
		metadata.put("state", "RESOLVED");

		GovernanceTimelineProjectionRecord stored =
				store.findRecent(1).blockFirst();

		assertThat(stored).isNotNull();
		assertThat(stored.metadata()).containsEntry("state", "OPEN");
		assertThatThrownBy(() -> stored.metadata().put("state", "MUTATED"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private GovernanceTimelineProjectionRecord record(
			String eventId,
			String occurredAt
	) {
		return new GovernanceTimelineProjectionRecord(
				eventId,
				"INCIDENT_TRANSITIONED",
				Instant.parse(occurredAt),
				"INCIDENT_LIFECYCLE",
				eventId + "-source",
				null,
				null,
				null,
				null,
				"INFO",
				"SYSTEM",
				"INCIDENT",
				"title-" + eventId,
				"summary-" + eventId,
				Map.of("eventId", eventId),
				false,
				Instant.parse("2026-05-17T00:00:01Z")
		);
	}
}
