package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceTimelineProjectionMetricsRecorderTest {

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

	@Test
	void shouldRecordAppendedWrite() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionMetricsRecorder recorder =
				new GovernanceTimelineProjectionMetricsRecorder(registry);

		recorder.write(GovernanceTimelineProjectionWriteStatus.APPENDED);

		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "appended")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRecordDuplicateSkippedWrite() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionMetricsRecorder recorder =
				new GovernanceTimelineProjectionMetricsRecorder(registry);

		recorder.write(GovernanceTimelineProjectionWriteStatus.DUPLICATE_SKIPPED);

		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "duplicate_skipped")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRecordRejectedWrite() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionMetricsRecorder recorder =
				new GovernanceTimelineProjectionMetricsRecorder(registry);

		recorder.write(GovernanceTimelineProjectionWriteStatus.REJECTED);

		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
						.tag("result", "rejected")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRecordFailure() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionMetricsRecorder recorder =
				new GovernanceTimelineProjectionMetricsRecorder(registry);

		recorder.failure();

		assertThat(
				registry.get(GovernanceTimelineProjectionMetricName.WRITE_FAILURE_TOTAL)
						.tag("result", "failure")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRejectNullStatus() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionMetricsRecorder recorder =
				new GovernanceTimelineProjectionMetricsRecorder(registry);

		assertThatThrownBy(() -> recorder.write(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("status must not be null");
	}

	private void assertNoForbiddenTagKeys(Iterable<Meter> meters) {
		for (Meter meter : meters) {
			assertThat(meter.getId().getTags())
					.noneMatch(tag -> FORBIDDEN_TAG_KEYS.contains(tag.getKey()));
		}
	}
}
