package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class GovernanceTimelineRuntimeSummaryService {

	private final GovernanceTimelineHealthService healthService;
	private final GovernanceTimelineRuntimeMetricsRecorder metricsRecorder;

	public GovernanceTimelineRuntimeSummaryService(
			GovernanceTimelineHealthService healthService,
			GovernanceTimelineRuntimeMetricsRecorder metricsRecorder
	) {
		this.healthService = healthService;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceTimelineRuntimeSummaryResponse> summary() {
		return healthService.health()
				.map(health -> {
					GovernanceTimelineRuntimeMode runtimeMode = runtimeMode(
							health.status()
					);
					GovernanceTimelineRuntimeSummaryResponse response =
							new GovernanceTimelineRuntimeSummaryResponse(
									Instant.now(),
									runtimeMode,
									health.status(),
									health.resilienceMode(),
									health.partialTimelineSupported(),
									health.failOpenReadOnly(),
									health.streamingCompatible(),
									degradedSignals(health),
									message(runtimeMode)
							);
					metricsRecorder.record(response);
					return response;
				});
	}

	private GovernanceTimelineRuntimeMode runtimeMode(
			GovernanceTimelineHealthStatus status
	) {
		if (status == null) {
			return GovernanceTimelineRuntimeMode.ATTENTION_REQUIRED;
		}

		return switch (status) {
			case HEALTHY -> GovernanceTimelineRuntimeMode.NORMAL;
			case DEGRADED_CAPABLE -> GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY;
			case STRICT, UNAVAILABLE ->
					GovernanceTimelineRuntimeMode.ATTENTION_REQUIRED;
		};
	}

	private List<String> degradedSignals(
			GovernanceTimelineHealthResponse health
	) {
		List<String> signals = new ArrayList<>();

		if (health == null) {
			return List.of("timeline:UNAVAILABLE");
		}

		if (health.status() != null
				&& health.status() != GovernanceTimelineHealthStatus.HEALTHY) {
			signals.add("timeline:" + health.status().name());
		}

		if (health.resilienceMode() != null
				&& health.resilienceMode() != GovernanceTimelineResilienceMode.STRICT) {
			signals.add("timeline:" + health.resilienceMode().name());
		}

		if (health.failOpenReadOnly()) {
			signals.add("timeline:FAIL_OPEN_READ_ONLY");
		}

		return List.copyOf(signals);
	}

	private String message(GovernanceTimelineRuntimeMode runtimeMode) {
		return switch (runtimeMode) {
			case NORMAL -> "Governance timeline runtime is normal.";
			case DEGRADED_READ_ONLY ->
					"Governance timeline runtime is degraded but read-only safe.";
			case ATTENTION_REQUIRED ->
					"Governance timeline runtime requires operational attention.";
		};
	}
}
