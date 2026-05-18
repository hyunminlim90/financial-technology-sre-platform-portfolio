package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceTimelineProjectionQueryMetricsRecorderTest {

	private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
			"cursor",
			"eventId",
			"sourceId",
			"incidentId",
			"recommendationRecordId",
			"learningCandidateId",
			"knowledgeUpdateApplicationId",
			"query",
			"exception",
			"exceptionMessage",
			"summary",
			"metadata",
			"rawPayload"
	);

	@Test
	void shouldRecordSuccessQuery() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionQueryMetricsRecorder recorder =
				new GovernanceTimelineProjectionQueryMetricsRecorder(registry);

		recorder.query("success", GovernanceCursorDirection.NEXT);

		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
						.tag("result", "success")
						.tag("direction", "NEXT")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRecordEmptyQuery() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionQueryMetricsRecorder recorder =
				new GovernanceTimelineProjectionQueryMetricsRecorder(registry);

		recorder.query("empty", GovernanceCursorDirection.PREVIOUS);

		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
						.tag("result", "empty")
						.tag("direction", "PREVIOUS")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRecordInvalidCursorQuery() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionQueryMetricsRecorder recorder =
				new GovernanceTimelineProjectionQueryMetricsRecorder(registry);

		recorder.query("invalid_cursor", GovernanceCursorDirection.NEXT);

		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
						.tag("result", "invalid_cursor")
						.tag("direction", "NEXT")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRecordFailure() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionQueryMetricsRecorder recorder =
				new GovernanceTimelineProjectionQueryMetricsRecorder(registry);

		recorder.failure(GovernanceCursorDirection.NEXT);

		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.QUERY_FAILURE_TOTAL)
						.tag("result", "failure")
						.tag("direction", "NEXT")
						.counter()
						.count()
		).isEqualTo(1.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRecordPageSize() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionQueryMetricsRecorder recorder =
				new GovernanceTimelineProjectionQueryMetricsRecorder(registry);

		recorder.pageSize(GovernanceCursorDirection.PREVIOUS, 3);

		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.PAGE_SIZE)
						.tag("direction", "PREVIOUS")
						.summary()
						.count()
		).isEqualTo(1L);
		assertThat(
				registry.get(GovernanceTimelineProjectionQueryMetricName.PAGE_SIZE)
						.tag("direction", "PREVIOUS")
						.summary()
						.totalAmount()
		).isEqualTo(3.0);
		assertNoForbiddenTagKeys(registry.getMeters());
	}

	@Test
	void shouldRejectNullResult() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionQueryMetricsRecorder recorder =
				new GovernanceTimelineProjectionQueryMetricsRecorder(registry);

		assertThatThrownBy(() -> recorder.query(null, GovernanceCursorDirection.NEXT))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("result must not be null");
	}

	@Test
	void shouldRejectNullDirectionForFailure() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineProjectionQueryMetricsRecorder recorder =
				new GovernanceTimelineProjectionQueryMetricsRecorder(registry);

		assertThatThrownBy(() -> recorder.failure(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("direction must not be null");
	}

	private void assertNoForbiddenTagKeys(Iterable<Meter> meters) {
		for (Meter meter : meters) {
			assertThat(meter.getId().getTags())
					.noneMatch(tag -> FORBIDDEN_TAG_KEYS.contains(tag.getKey()));
		}
	}
}
