package com.fintech.sre.agent.governance.timeline;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceTimelineRuntimeMetricsRecorder {

	private final AtomicReference<GovernanceTimelineRuntimeSummaryResponse> latest =
			new AtomicReference<>();

	public GovernanceTimelineRuntimeMetricsRecorder(
			MeterRegistry meterRegistry
	) {
		Gauge.builder(
						GovernanceTimelineMetricName.RUNTIME_MODE,
						latest,
						ref -> GovernanceTimelineRuntimeModeValue.valueOf(
								ref.get() == null
										? GovernanceTimelineRuntimeMode.ATTENTION_REQUIRED
										: ref.get().runtimeMode()
						)
				)
				.description("""
						Governance timeline runtime mode.
						NORMAL=0,
						DEGRADED_READ_ONLY=1,
						ATTENTION_REQUIRED=2
						""")
				.tag("component", "governance-timeline-runtime")
				.register(meterRegistry);
	}

	public void record(GovernanceTimelineRuntimeSummaryResponse response) {
		if (response == null) {
			return;
		}

		latest.set(response);
	}
}
