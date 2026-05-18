package com.fintech.sre.agent.governance.dashboard;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceDashboardHealthMetricsRecorder {

	private final AtomicReference<GovernanceDashboardHealthResponse> latest =
			new AtomicReference<>();

	public GovernanceDashboardHealthMetricsRecorder(
			MeterRegistry meterRegistry
	) {
		Gauge.builder(
						GovernanceDashboardMetricName.HEALTH_STATUS,
						latest,
						ref -> GovernanceDashboardHealthStatusValue.valueOf(
								ref.get() == null
										? GovernanceDashboardHealthStatus.UNAVAILABLE
										: ref.get().status()
						)
				)
				.description("Governance dashboard health status. HEALTHY=0, DEGRADED=1, UNAVAILABLE=2.")
				.tag("component", "governance-dashboard")
				.register(meterRegistry);
	}

	public void record(GovernanceDashboardHealthResponse response) {
		if (response == null) {
			return;
		}

		latest.set(response);
	}
}
