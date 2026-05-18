package com.fintech.sre.agent.governance.detail;

import java.time.Instant;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class GovernanceDetailHealthService {

	private final GovernanceDetailResilienceProperties properties;
	private final GovernanceDetailHealthMetricsRecorder metricsRecorder;

	public GovernanceDetailHealthService(
			GovernanceDetailResilienceProperties properties,
			GovernanceDetailHealthMetricsRecorder metricsRecorder
	) {
		this.properties = properties;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceDetailHealthResponse> health() {
		boolean enabled = properties.isEnabled();
		boolean partial = properties.isPartialResponseEnabled();
		boolean failOpen = properties.isFailOpenDetail();

		GovernanceDetailHealthStatus status;
		String message;

		if (!enabled) {
			status = GovernanceDetailHealthStatus.HEALTHY;
			message =
					"Governance detail APIs are available with default strict component behavior.";
		} else if (partial && failOpen) {
			status = GovernanceDetailHealthStatus.DEGRADED_CAPABLE;
			message =
					"Governance detail APIs may return partial degraded responses for child component failures.";
		} else {
			status = GovernanceDetailHealthStatus.STRICT;
			message =
					"Governance detail APIs use strict failure behavior for child component failures.";
		}

		GovernanceDetailHealthResponse response = new GovernanceDetailHealthResponse(
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
