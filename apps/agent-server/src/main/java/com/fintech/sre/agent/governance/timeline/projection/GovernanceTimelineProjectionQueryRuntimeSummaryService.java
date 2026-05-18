package com.fintech.sre.agent.governance.timeline.projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import reactor.core.publisher.Mono;

public class GovernanceTimelineProjectionQueryRuntimeSummaryService {

	private final GovernanceTimelineProjectionQueryHealthService healthService;

	public GovernanceTimelineProjectionQueryRuntimeSummaryService(
			GovernanceTimelineProjectionQueryHealthService healthService
	) {
		this.healthService = Objects.requireNonNull(
				healthService,
				"healthService must not be null"
		);
	}

	public Mono<GovernanceTimelineProjectionQueryRuntimeSummary> summary() {
		return healthService.health()
				.map(health -> new GovernanceTimelineProjectionQueryRuntimeSummary(
						health.checkedAt(),
						runtimeMode(health.status()),
						health.aggregationMode(),
						health.projectionBackedAvailable(),
						health.lightweight(),
						signals(health),
						summary(runtimeMode(health.status()))
				));
	}

	private GovernanceTimelineProjectionQueryRuntimeMode runtimeMode(
			GovernanceTimelineProjectionQueryHealthStatus status
	) {
		if (status == null) {
			return GovernanceTimelineProjectionQueryRuntimeMode.ATTENTION_REQUIRED;
		}
		return switch (status) {
			case HEALTHY -> GovernanceTimelineProjectionQueryRuntimeMode.NORMAL;
			case DEGRADED ->
					GovernanceTimelineProjectionQueryRuntimeMode.DEGRADED_READ_ONLY;
			case UNAVAILABLE ->
					GovernanceTimelineProjectionQueryRuntimeMode.ATTENTION_REQUIRED;
		};
	}

	private List<String> signals(
			GovernanceTimelineProjectionQueryHealthResponse health
	) {
		List<String> signals = new ArrayList<>();
		if (health != null && health.status() != null) {
			signals.add("projection-query:" + health.status().name());
		}
		if (health != null && health.aggregationMode() != null) {
			signals.add("aggregation-mode:" + health.aggregationMode().name());
		}
		return List.copyOf(signals);
	}

	private String summary(
			GovernanceTimelineProjectionQueryRuntimeMode runtimeMode
	) {
		return switch (runtimeMode) {
			case NORMAL -> "Projection-backed timeline query runtime is normal.";
			case DEGRADED_READ_ONLY ->
					"Projection-backed timeline query runtime is degraded but read-only safe.";
			case ATTENTION_REQUIRED ->
					"Projection-backed timeline query runtime requires operational attention.";
		};
	}
}
