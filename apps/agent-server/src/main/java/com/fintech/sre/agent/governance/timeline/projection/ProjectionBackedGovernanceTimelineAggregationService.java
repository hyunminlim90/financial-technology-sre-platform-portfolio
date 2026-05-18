package com.fintech.sre.agent.governance.timeline.projection;

import java.util.Objects;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationRequest;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationResult;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationService;

import reactor.core.publisher.Mono;

public class ProjectionBackedGovernanceTimelineAggregationService
		implements GovernanceTimelineAggregationService {

	private final GovernanceTimelineProjectionQueryAdapter queryAdapter;

	public ProjectionBackedGovernanceTimelineAggregationService(
			GovernanceTimelineProjectionQueryAdapter queryAdapter
	) {
		this.queryAdapter = Objects.requireNonNull(
				queryAdapter,
				"queryAdapter must not be null"
		);
	}

	@Override
	public Mono<GovernanceTimelineAggregationResult> aggregate(
			GovernanceTimelineAggregationRequest request
	) {
		Objects.requireNonNull(request, "request must not be null");
		return queryAdapter.query(request);
	}
}
