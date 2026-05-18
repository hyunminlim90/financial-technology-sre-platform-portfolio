package com.fintech.sre.agent.governance.console;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthService;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthStatus;
import com.fintech.sre.agent.governance.detail.GovernanceDetailHealthService;
import com.fintech.sre.agent.governance.detail.GovernanceDetailHealthStatus;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthService;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthStatus;

import reactor.core.publisher.Mono;

@Service
public class GovernanceConsoleHealthService {

	private final GovernanceDashboardHealthService dashboardHealthService;
	private final GovernanceDetailHealthService detailHealthService;
	private final GovernanceSearchHealthService searchHealthService;
	private final GovernanceConsoleHealthMetricsRecorder metricsRecorder;

	public GovernanceConsoleHealthService(
			GovernanceDashboardHealthService dashboardHealthService,
			GovernanceDetailHealthService detailHealthService,
			GovernanceSearchHealthService searchHealthService,
			GovernanceConsoleHealthMetricsRecorder metricsRecorder
	) {
		this.dashboardHealthService = dashboardHealthService;
		this.detailHealthService = detailHealthService;
		this.searchHealthService = searchHealthService;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceConsoleHealthResponse> health() {
		return Mono.zip(
				dashboardHealthService.health(),
				detailHealthService.health(),
				searchHealthService.health()
		).map(tuple -> {
			GovernanceConsoleHealthStatus overall = overall(
					tuple.getT1().status(),
					tuple.getT2().status(),
					tuple.getT3().status()
			);

			GovernanceConsoleHealthResponse response = new GovernanceConsoleHealthResponse(
					Instant.now(),
					overall,
					tuple.getT1(),
					tuple.getT2(),
					tuple.getT3(),
					message(overall)
			);

			metricsRecorder.record(response);
			return response;
		});
	}

	private GovernanceConsoleHealthStatus overall(
			GovernanceDashboardHealthStatus dashboard,
			GovernanceDetailHealthStatus detail,
			GovernanceSearchHealthStatus search
	) {
		if (dashboard == GovernanceDashboardHealthStatus.UNAVAILABLE
				|| detail == GovernanceDetailHealthStatus.STRICT
				|| search == GovernanceSearchHealthStatus.STRICT) {
			return GovernanceConsoleHealthStatus.ATTENTION_REQUIRED;
		}

		if (dashboard == GovernanceDashboardHealthStatus.DEGRADED
				|| detail == GovernanceDetailHealthStatus.DEGRADED_CAPABLE
				|| search == GovernanceSearchHealthStatus.DEGRADED_CAPABLE) {
			return GovernanceConsoleHealthStatus.DEGRADED;
		}

		return GovernanceConsoleHealthStatus.HEALTHY;
	}

	private String message(GovernanceConsoleHealthStatus status) {
		return switch (status) {
			case HEALTHY -> "Governance Console is healthy.";
			case DEGRADED ->
					"Governance Console is available with degraded-capable components.";
			case ATTENTION_REQUIRED ->
					"Governance Console requires operational attention.";
		};
	}
}
