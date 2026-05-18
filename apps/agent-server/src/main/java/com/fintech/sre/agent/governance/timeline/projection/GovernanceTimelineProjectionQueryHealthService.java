package com.fintech.sre.agent.governance.timeline.projection;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationModeProperties;

import reactor.core.publisher.Mono;

public class GovernanceTimelineProjectionQueryHealthService {

	private final GovernanceTimelineProjectionQueryAdapter queryAdapter;
	private final GovernanceTimelineAggregationModeProperties properties;
	private final Clock clock;

	public GovernanceTimelineProjectionQueryHealthService(
			GovernanceTimelineProjectionQueryAdapter queryAdapter,
			GovernanceTimelineAggregationModeProperties properties,
			Clock clock
	) {
		this.queryAdapter = queryAdapter;
		this.properties = Objects.requireNonNull(
				properties,
				"properties must not be null"
		);
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	public Mono<GovernanceTimelineProjectionQueryHealthResponse> health() {
		boolean available = queryAdapter != null;
		GovernanceTimelineProjectionQueryHealthStatus status = available
				? GovernanceTimelineProjectionQueryHealthStatus.HEALTHY
				: GovernanceTimelineProjectionQueryHealthStatus.UNAVAILABLE;

		return Mono.just(new GovernanceTimelineProjectionQueryHealthResponse(
				Instant.now(clock),
				status,
				properties.getMode(),
				available,
				true,
				message(status)
		));
	}

	private String message(GovernanceTimelineProjectionQueryHealthStatus status) {
		return switch (status) {
			case HEALTHY -> "Projection-backed timeline query path is available.";
			case DEGRADED -> "Projection-backed timeline query path is degraded.";
			case UNAVAILABLE -> "Projection-backed timeline query path is unavailable.";
		};
	}
}
