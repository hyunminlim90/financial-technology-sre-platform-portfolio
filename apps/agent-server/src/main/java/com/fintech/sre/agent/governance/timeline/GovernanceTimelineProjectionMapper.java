package com.fintech.sre.agent.governance.timeline;

import java.util.Optional;

public interface GovernanceTimelineProjectionMapper {

	Optional<GovernanceTimelineProjection> project(Object source);
}
