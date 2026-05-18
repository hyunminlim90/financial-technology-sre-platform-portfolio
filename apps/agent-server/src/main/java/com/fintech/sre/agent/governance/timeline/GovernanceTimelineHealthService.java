package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class GovernanceTimelineHealthService {

	private static final List<String> DEGRADED_REASON_TAXONOMY = List.of(
			"component_query_failed",
			"component_query_timeout",
			"projection_failed",
			"aggregation_degraded",
			"timeline_query_timeout"
	);

	private final ObjectProvider<GovernanceTimelineAggregationService> aggregationServiceProvider;
	private final GovernanceTimelineResilienceProperties resilienceProperties;
	private final GovernanceTimelineHealthMetricsRecorder metricsRecorder;

	public GovernanceTimelineHealthService(
			ObjectProvider<GovernanceTimelineAggregationService> aggregationServiceProvider,
			GovernanceTimelineResilienceProperties resilienceProperties,
			GovernanceTimelineHealthMetricsRecorder metricsRecorder
	) {
		this.aggregationServiceProvider = aggregationServiceProvider;
		this.resilienceProperties = resilienceProperties;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceTimelineHealthResponse> health() {
		if (aggregationServiceProvider.getIfAvailable() == null) {
			GovernanceTimelineHealthResponse response =
					new GovernanceTimelineHealthResponse(
							Instant.now(),
							GovernanceTimelineHealthStatus.UNAVAILABLE,
							GovernanceTimelineResilienceMode.STRICT,
							false,
							false,
							true,
							DEGRADED_REASON_TAXONOMY,
							"Governance timeline query and aggregation layer is unavailable."
					);
			metricsRecorder.record(response);
			return Mono.just(response);
		}

		boolean enabled = resilienceProperties.isEnabled();
		boolean partialTimelineEnabled =
				enabled && resilienceProperties.isPartialTimelineEnabled();
		boolean failOpenReadOnly =
				enabled
						&& resilienceProperties.isPartialTimelineEnabled()
						&& resilienceProperties.isFailOpenReadOnly();

		GovernanceTimelineHealthStatus status;
		GovernanceTimelineResilienceMode resilienceMode;
		String message;

		if (!enabled) {
			status = GovernanceTimelineHealthStatus.HEALTHY;
			resilienceMode = GovernanceTimelineResilienceMode.STRICT;
			message =
					"Governance timeline query and aggregation contract is available with normal strict read-only behavior.";
		}
		else if (resilienceProperties.isPartialTimelineEnabled()
				&& resilienceProperties.isFailOpenReadOnly()) {
			status = GovernanceTimelineHealthStatus.DEGRADED_CAPABLE;
			resilienceMode = GovernanceTimelineResilienceMode.FAIL_OPEN_READ_ONLY;
			message =
					"Governance timeline may return partial degraded read-only responses when component queries fail.";
		}
		else if (resilienceProperties.isPartialTimelineEnabled()) {
			status = GovernanceTimelineHealthStatus.STRICT;
			resilienceMode = GovernanceTimelineResilienceMode.PARTIAL_DEGRADED;
			message =
					"Governance timeline supports partial degraded semantics but uses strict failure behavior when fail-open read-only is disabled.";
		}
		else {
			status = GovernanceTimelineHealthStatus.STRICT;
			resilienceMode = GovernanceTimelineResilienceMode.STRICT;
			message =
					"Governance timeline uses strict failure behavior for component failures.";
		}

		GovernanceTimelineHealthResponse response = new GovernanceTimelineHealthResponse(
				Instant.now(),
				status,
				resilienceMode,
				partialTimelineEnabled,
				failOpenReadOnly,
				true,
				DEGRADED_REASON_TAXONOMY,
				message
		);
		metricsRecorder.record(response);
		return Mono.just(response);
	}
}
