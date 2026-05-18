package com.fintech.sre.agent.governance.timeline;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceTimelineHealthMetricsRecorder {

	private final AtomicReference<GovernanceTimelineHealthResponse> latest =
			new AtomicReference<>();

	public GovernanceTimelineHealthMetricsRecorder(
			MeterRegistry meterRegistry
	) {
		Gauge.builder(
						GovernanceTimelineMetricName.HEALTH_STATUS,
						latest,
						ref -> GovernanceTimelineHealthStatusValue.valueOf(
								ref.get() == null
										? GovernanceTimelineHealthStatus.UNAVAILABLE
										: ref.get().status()
						)
				)
				.description("""
						Governance timeline health status.
						HEALTHY=0,
						DEGRADED_CAPABLE=1,
						STRICT=2,
						UNAVAILABLE=3
						""")
				.tag("component", "governance-timeline")
				.register(meterRegistry);
	}

	public void record(GovernanceTimelineHealthResponse response) {
		if (response == null) {
			return;
		}
		latest.set(response);
	}
}
