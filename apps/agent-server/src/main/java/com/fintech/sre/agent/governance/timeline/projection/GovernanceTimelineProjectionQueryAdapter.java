package com.fintech.sre.agent.governance.timeline.projection;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationRequest;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationResult;

import reactor.core.publisher.Mono;

public interface GovernanceTimelineProjectionQueryAdapter {

	Mono<GovernanceTimelineAggregationResult> query(
			GovernanceTimelineAggregationRequest request
	);
}
