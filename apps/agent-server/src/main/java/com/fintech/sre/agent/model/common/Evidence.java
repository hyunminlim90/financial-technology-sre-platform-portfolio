package com.fintech.sre.agent.model.common;

import java.util.List;

public record Evidence(
		List<MetricEvidence> metrics,
		List<String> logs,
		List<String> traces
) {
}
