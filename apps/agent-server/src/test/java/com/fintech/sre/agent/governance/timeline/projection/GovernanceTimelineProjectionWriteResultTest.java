package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionWriteResultTest {

	@Test
	void shouldCreateAppendedResult() {
		GovernanceTimelineProjectionWriteResult result =
				GovernanceTimelineProjectionWriteResult.appended("event-1");

		assertThat(result.status())
				.isEqualTo(GovernanceTimelineProjectionWriteStatus.APPENDED);
		assertThat(result.eventId()).isEqualTo("event-1");
	}

	@Test
	void shouldCreateDuplicateSkippedResult() {
		GovernanceTimelineProjectionWriteResult result =
				GovernanceTimelineProjectionWriteResult.duplicateSkipped("event-2");

		assertThat(result.status())
				.isEqualTo(
						GovernanceTimelineProjectionWriteStatus.DUPLICATE_SKIPPED
				);
		assertThat(result.eventId()).isEqualTo("event-2");
	}

	@Test
	void shouldCreateRejectedResult() {
		GovernanceTimelineProjectionWriteResult result =
				GovernanceTimelineProjectionWriteResult.rejected("event-3");

		assertThat(result.status())
				.isEqualTo(GovernanceTimelineProjectionWriteStatus.REJECTED);
		assertThat(result.eventId()).isEqualTo("event-3");
	}
}
