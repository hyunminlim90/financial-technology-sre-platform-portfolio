package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class DefaultGovernanceTimelineProjectionWriterTest {

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
			Instant.parse("2026-05-17T02:03:04Z"),
			ZoneOffset.UTC
	);

	private final GovernanceTimelineProjectionRecordMapper mapper =
			new DefaultGovernanceTimelineProjectionRecordMapper(fixedClock);

	@Test
	void shouldWriteProjectionAsAppended() {
		InMemoryGovernanceTimelineProjectionStore store =
				new InMemoryGovernanceTimelineProjectionStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		DefaultGovernanceTimelineProjectionWriter writer =
				new DefaultGovernanceTimelineProjectionWriter(
						mapper,
						store,
						new GovernanceTimelineProjectionMetricsRecorder(registry)
				);

		GovernanceTimelineProjectionWriteResult result = writer.write(
				projection("event-1", "source-1")
		).block();

		assertThat(result).isNotNull();
		assertThat(result.status())
				.isEqualTo(GovernanceTimelineProjectionWriteStatus.APPENDED);
		assertThat(result.eventId()).isEqualTo("event-1");

		GovernanceTimelineProjectionRecord stored = store.findRecent(1).blockFirst();
		assertThat(stored).isNotNull();
		assertThat(stored.eventId()).isEqualTo("event-1");
		assertThat(stored.sourceId()).isEqualTo("source-1");
		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "appended")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldReturnDuplicateSkippedWhenStoreSeesDuplicate() {
		InMemoryGovernanceTimelineProjectionStore store =
				new InMemoryGovernanceTimelineProjectionStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		DefaultGovernanceTimelineProjectionWriter writer =
				new DefaultGovernanceTimelineProjectionWriter(
						mapper,
						store,
						new GovernanceTimelineProjectionMetricsRecorder(registry)
				);

		writer.write(projection("event-1", "source-1")).block();
		GovernanceTimelineProjectionWriteResult result = writer.write(
				projection("event-1", "source-1")
		).block();

		assertThat(result).isNotNull();
		assertThat(result.status())
				.isEqualTo(
						GovernanceTimelineProjectionWriteStatus.DUPLICATE_SKIPPED
				);
		assertThat(result.eventId()).isEqualTo("event-1");
		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "duplicate_skipped")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldPassMappedRecordToStoreAppend() {
		CapturingProjectionStore store = new CapturingProjectionStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		DefaultGovernanceTimelineProjectionWriter writer =
				new DefaultGovernanceTimelineProjectionWriter(
						mapper,
						store,
						new GovernanceTimelineProjectionMetricsRecorder(registry)
				);

		writer.write(projection("event-2", "source-2")).block();

		assertThat(store.captured).isNotNull();
		assertThat(store.captured.eventId()).isEqualTo("event-2");
		assertThat(store.captured.eventType()).isEqualTo("RECOMMENDATION_CREATED");
		assertThat(store.captured.sourceType()).isEqualTo("RECOMMENDATION_RECORD");
		assertThat(store.captured.sourceId()).isEqualTo("source-2");
		assertThat(store.captured.recommendationRecordId()).isEqualTo("rec-1");
		assertThat(store.captured.createdAt()).isEqualTo(Instant.parse("2026-05-17T02:03:04Z"));
		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "appended")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRejectNullProjection() {
		DefaultGovernanceTimelineProjectionWriter writer =
				new DefaultGovernanceTimelineProjectionWriter(
						mapper,
						new InMemoryGovernanceTimelineProjectionStore(),
						new GovernanceTimelineProjectionMetricsRecorder(
								new SimpleMeterRegistry()
						)
				);

		assertThatThrownBy(() -> writer.write(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("projection must not be null");
	}

	@Test
	void shouldRejectNullMapperDependency() {
		assertThatThrownBy(
				() -> new DefaultGovernanceTimelineProjectionWriter(
						null,
						new InMemoryGovernanceTimelineProjectionStore(),
						new GovernanceTimelineProjectionMetricsRecorder(
								new SimpleMeterRegistry()
						)
				)
		)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("mapper must not be null");
	}

	@Test
	void shouldRejectNullStoreDependency() {
		assertThatThrownBy(
				() -> new DefaultGovernanceTimelineProjectionWriter(
						mapper,
						null,
						new GovernanceTimelineProjectionMetricsRecorder(
								new SimpleMeterRegistry()
						)
				)
		)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("store must not be null");
	}

	@Test
	void shouldRejectNullMetricsRecorderDependency() {
		assertThatThrownBy(
				() -> new DefaultGovernanceTimelineProjectionWriter(
						mapper,
						new InMemoryGovernanceTimelineProjectionStore(),
						null
				)
		)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("metricsRecorder must not be null");
	}

	@Test
	void shouldPropagateStoreFailure() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionStore failingStore =
				new GovernanceTimelineProjectionStore() {
					@Override
					public Mono<GovernanceTimelineProjectionWriteResult> append(
							GovernanceTimelineProjectionRecord record
					) {
						return Mono.error(new IllegalStateException("store failure"));
					}

					@Override
					public Flux<GovernanceTimelineProjectionRecord> findRecent(int limit) {
						return Flux.empty();
					}
				};
		DefaultGovernanceTimelineProjectionWriter writer =
				new DefaultGovernanceTimelineProjectionWriter(
						mapper,
						failingStore,
						new GovernanceTimelineProjectionMetricsRecorder(registry)
				);

		assertThatThrownBy(() -> writer.write(projection("event-1", "source-1")).block())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("store failure");
		assertThat(
				registry.get(
						GovernanceTimelineProjectionMetricName.WRITE_FAILURE_TOTAL
				)
						.tag("result", "failure")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	private GovernanceTimelineProjection projection(String eventId, String sourceId) {
		return new GovernanceTimelineProjection(
				GovernanceTimelineProjectionType.RECOMMENDATION_RECORD,
				sourceId,
				"incident-1",
				new GovernanceTimelineEvent(
						eventId,
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
	}

	private static final class CapturingProjectionStore
			implements GovernanceTimelineProjectionStore {

		private GovernanceTimelineProjectionRecord captured;

		@Override
		public Mono<GovernanceTimelineProjectionWriteResult> append(
				GovernanceTimelineProjectionRecord record
		) {
			this.captured = record;
			return Mono.just(
					GovernanceTimelineProjectionWriteResult.appended(record.eventId())
			);
		}

		@Override
		public Flux<GovernanceTimelineProjectionRecord> findRecent(int limit) {
			return Flux.empty();
		}
	}

	private void assertNoForbiddenTagKeys(Iterable<Meter> meters) {
		for (Meter meter : meters) {
			assertThat(meter.getId().getTags())
					.noneMatch(tag -> FORBIDDEN_TAG_KEYS.contains(tag.getKey()));
		}
	}
}
