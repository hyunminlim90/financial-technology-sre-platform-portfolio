package com.fintech.sre.agent.governance.console;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceConsoleRuntimeMetricsRecorder {

	private final AtomicReference<GovernanceConsoleRuntimeSummaryResponse> latest =
			new AtomicReference<>();

	public GovernanceConsoleRuntimeMetricsRecorder(
			MeterRegistry meterRegistry
	) {
		Gauge.builder(
						GovernanceConsoleMetricName.RUNTIME_MODE,
						latest,
						ref -> GovernanceConsoleRuntimeModeValue.valueOf(
								ref.get() == null
										? GovernanceConsoleRuntimeMode.ATTENTION_REQUIRED
										: ref.get().runtimeMode()
						)
				)
				.description("""
						Governance console runtime mode.
						NORMAL=0,
						DEGRADED_READ_ONLY=1,
						ATTENTION_REQUIRED=2
						""")
				.tag("component", "governance-console-runtime")
				.register(meterRegistry);
	}

	public void record(GovernanceConsoleRuntimeSummaryResponse response) {
		if (response == null) {
			return;
		}

		latest.set(response);
	}
}
