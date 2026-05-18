package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryRepository;
import com.fintech.sre.agent.governance.query.GovernanceQueryResilienceProperties;

import reactor.core.publisher.Mono;

@Service
public class GovernanceDashboardHealthService {

	private final Optional<GovernanceDashboardQueryRepository> queryRepository;
	private final GovernanceQueryResilienceProperties properties;
	private final GovernanceDashboardHealthMetricsRecorder metricsRecorder;

	public GovernanceDashboardHealthService(
			Optional<GovernanceDashboardQueryRepository> queryRepository,
			GovernanceQueryResilienceProperties properties,
			GovernanceDashboardHealthMetricsRecorder metricsRecorder
	) {
		this.queryRepository = queryRepository;
		this.properties = properties;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceDashboardHealthResponse> health() {
		boolean optimizedAvailable = queryRepository.isPresent();
		boolean fallbackEnabled = properties.isFallbackEnabled();
		boolean failOpen = properties.isFailOpenDashboard();
		boolean resilienceEnabled = properties.isEnabled();

		GovernanceDashboardHealthStatus status;
		String reason = "none";
		String message;

		if (optimizedAvailable) {
			status = GovernanceDashboardHealthStatus.HEALTHY;
			message = "Dashboard optimized query layer is available.";
		} else if (fallbackEnabled && failOpen) {
			status = GovernanceDashboardHealthStatus.DEGRADED;
			reason = "optimized_query_repository_missing";
			message = "Dashboard is available through fallback aggregation.";
		} else {
			status = GovernanceDashboardHealthStatus.UNAVAILABLE;
			reason = "optimized_query_repository_missing";
			message = "Dashboard optimized query layer is unavailable and fallback is disabled.";
		}

		GovernanceDashboardHealthResponse response = new GovernanceDashboardHealthResponse(
				Instant.now(),
				status,
				optimizedAvailable,
				fallbackEnabled,
				failOpen,
				resilienceEnabled,
				reason,
				message
		);
		metricsRecorder.record(response);
		return Mono.just(response);
	}
}
