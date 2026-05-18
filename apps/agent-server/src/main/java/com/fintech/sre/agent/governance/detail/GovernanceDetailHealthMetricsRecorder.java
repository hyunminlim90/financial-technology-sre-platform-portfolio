package com.fintech.sre.agent.governance.detail;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceDetailHealthMetricsRecorder {

	private final AtomicReference<GovernanceDetailHealthResponse> latest =
			new AtomicReference<>();

	public GovernanceDetailHealthMetricsRecorder(
			MeterRegistry meterRegistry
	) {
		Gauge.builder(
						GovernanceDetailMetricName.HEALTH_STATUS,
						latest,
						ref -> GovernanceDetailHealthStatusValue.valueOf(
								ref.get() == null
										? GovernanceDetailHealthStatus.STRICT
										: ref.get().status()
						)
				)
				.description("""
						Governance detail health status.
						HEALTHY=0,
						DEGRADED_CAPABLE=1,
						STRICT=2
						""")
				.tag("component", "governance-detail")
				.register(meterRegistry);
	}

	public void record(
			GovernanceDetailHealthResponse response
	) {
		if (response == null) {
			return;
		}

		latest.set(response);
	}
}
