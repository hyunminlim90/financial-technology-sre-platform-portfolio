package com.fintech.sre.agent.governance.search;

import java.time.Instant;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class GovernanceSearchHealthService {

	private final GovernanceSearchResilienceProperties properties;
	private final GovernanceSearchHealthMetricsRecorder metricsRecorder;

	public GovernanceSearchHealthService(
			GovernanceSearchResilienceProperties properties,
			GovernanceSearchHealthMetricsRecorder metricsRecorder
	) {
		this.properties = properties;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceSearchHealthResponse> health() {
		boolean enabled = properties.isEnabled();
		boolean partial = properties.isPartialSearchEnabled();
		boolean failOpen = properties.isFailOpenSearch();

		GovernanceSearchHealthStatus status;
		String message;

		if (!enabled) {
			status = GovernanceSearchHealthStatus.HEALTHY;
			message =
					"Governance search APIs are available with default strict component behavior.";
		} else if (partial && failOpen) {
			status = GovernanceSearchHealthStatus.DEGRADED_CAPABLE;
			message =
					"Governance search may return partial degraded results for ALL search component failures.";
		} else {
			status = GovernanceSearchHealthStatus.STRICT;
			message =
					"Governance search uses strict failure behavior for component failures.";
		}

		GovernanceSearchHealthResponse response = new GovernanceSearchHealthResponse(
				Instant.now(),
				status,
				enabled,
				partial,
				failOpen,
				properties.getComponentQueryTimeoutMs(),
				message
		);

		metricsRecorder.record(response);
		return Mono.just(response);
	}
}
