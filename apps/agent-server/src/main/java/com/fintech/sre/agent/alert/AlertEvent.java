package com.fintech.sre.agent.alert;

import java.time.Instant;
import java.util.Map;

public record AlertEvent(
		String alertId,
		AlertSource source,
		String alertName,
		AlertSeverity severity,
		String status,
		String service,
		String domain,
		String namespace,
		String description,
		Instant startsAt,
		Instant endsAt,
		Map<String, String> labels,
		Map<String, String> annotations
) {
}
