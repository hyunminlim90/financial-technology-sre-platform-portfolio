package com.fintech.sre.agent.governance.search;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceSearchHealthMetricsRecorder {

	private final AtomicReference<GovernanceSearchHealthResponse> latest =
			new AtomicReference<>();

	public GovernanceSearchHealthMetricsRecorder(
			MeterRegistry meterRegistry
	) {
		Gauge.builder(
						GovernanceSearchMetricName.HEALTH_STATUS,
						latest,
						ref -> GovernanceSearchHealthStatusValue.valueOf(
								ref.get() == null
										? GovernanceSearchHealthStatus.STRICT
										: ref.get().status()
						)
				)
				.description("""
						Governance search health status.
						HEALTHY=0,
						DEGRADED_CAPABLE=1,
						STRICT=2
						""")
				.tag("component", "governance-search")
				.register(meterRegistry);
	}

	public void record(GovernanceSearchHealthResponse response) {
		if (response == null) {
			return;
		}

		latest.set(response);
	}
}
