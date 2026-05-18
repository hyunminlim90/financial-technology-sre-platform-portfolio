package com.fintech.sre.agent.governance.console;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceConsoleHealthMetricsRecorder {

	private final AtomicReference<GovernanceConsoleHealthResponse> latest =
			new AtomicReference<>();

	public GovernanceConsoleHealthMetricsRecorder(
			MeterRegistry meterRegistry
	) {
		Gauge.builder(
						GovernanceConsoleMetricName.HEALTH_STATUS,
						latest,
						ref -> GovernanceConsoleHealthStatusValue.valueOf(
								ref.get() == null
										? GovernanceConsoleHealthStatus.ATTENTION_REQUIRED
										: ref.get().overallStatus()
						)
				)
				.description("""
						Governance console health status.
						HEALTHY=0,
						DEGRADED=1,
						ATTENTION_REQUIRED=2
						""")
				.tag("component", "governance-console")
				.register(meterRegistry);
	}

	public void record(GovernanceConsoleHealthResponse response) {
		if (response == null) {
			return;
		}

		latest.set(response);
	}
}
