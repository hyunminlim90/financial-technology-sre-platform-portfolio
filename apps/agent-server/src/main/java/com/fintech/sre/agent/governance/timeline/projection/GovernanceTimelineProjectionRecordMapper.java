package com.fintech.sre.agent.governance.timeline.projection;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineProjection;

public interface GovernanceTimelineProjectionRecordMapper {

	GovernanceTimelineProjectionRecord map(
			GovernanceTimelineProjection projection
	);
}
