package com.fintech.sre.agent.governance.timeline;

import java.util.Objects;

import reactor.core.publisher.Mono;

public class RoutingGovernanceTimelineAggregationService
		implements GovernanceTimelineAggregationService {

	private final GovernanceTimelineAggregationModeProperties properties;
	private final GovernanceTimelineAggregationService runtimeAggregationService;
	private final GovernanceTimelineAggregationService projectionBackedAggregationService;

	public RoutingGovernanceTimelineAggregationService(
			GovernanceTimelineAggregationModeProperties properties,
			GovernanceTimelineAggregationService runtimeAggregationService,
			GovernanceTimelineAggregationService projectionBackedAggregationService
	) {
		this.properties = Objects.requireNonNull(
				properties,
				"properties must not be null"
		);
		this.runtimeAggregationService = Objects.requireNonNull(
				runtimeAggregationService,
				"runtimeAggregationService must not be null"
		);
		this.projectionBackedAggregationService = Objects.requireNonNull(
				projectionBackedAggregationService,
				"projectionBackedAggregationService must not be null"
		);
	}

	@Override
	public Mono<GovernanceTimelineAggregationResult> aggregate(
			GovernanceTimelineAggregationRequest request
	) {
		Objects.requireNonNull(request, "request must not be null");

		return switch (properties.getMode()) {
			case RUNTIME_FAN_OUT -> runtimeAggregationService.aggregate(request);
			case PROJECTION_BACKED -> projectionBackedAggregationService.aggregate(request);
		};
	}
}
