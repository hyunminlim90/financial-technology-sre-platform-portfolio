package com.fintech.sre.agent.governance.timeline;

import reactor.core.publisher.Mono;

public interface GovernanceTimelineAggregationService {

	Mono<GovernanceTimelineAggregationResult> aggregate(
			GovernanceTimelineAggregationRequest request
	);
}
