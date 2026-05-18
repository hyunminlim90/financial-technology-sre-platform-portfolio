package com.fintech.sre.agent.governance.timeline.projection;

public record GovernanceTimelineProjectionWriteResult(
		GovernanceTimelineProjectionWriteStatus status,
		String eventId
) {

	public static GovernanceTimelineProjectionWriteResult appended(
			String eventId
	) {
		return new GovernanceTimelineProjectionWriteResult(
				GovernanceTimelineProjectionWriteStatus.APPENDED,
				eventId
		);
	}

	public static GovernanceTimelineProjectionWriteResult duplicateSkipped(
			String eventId
	) {
		return new GovernanceTimelineProjectionWriteResult(
				GovernanceTimelineProjectionWriteStatus.DUPLICATE_SKIPPED,
				eventId
		);
	}

	public static GovernanceTimelineProjectionWriteResult rejected(
			String eventId
	) {
		return new GovernanceTimelineProjectionWriteResult(
				GovernanceTimelineProjectionWriteStatus.REJECTED,
				eventId
		);
	}
}
