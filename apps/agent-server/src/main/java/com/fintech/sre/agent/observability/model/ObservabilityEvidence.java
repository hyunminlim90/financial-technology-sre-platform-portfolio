package com.fintech.sre.agent.observability.model;

import java.util.List;

public record ObservabilityEvidence(
		List<MetricEvidence> metrics,
		List<LogEvidence> logs,
		List<TraceEvidence> traces
) {

	public boolean hasAnyEvidence() {
		return (metrics != null && !metrics.isEmpty())
				|| (logs != null && !logs.isEmpty())
				|| (traces != null && !traces.isEmpty());
	}
}
