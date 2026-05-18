package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.List;

public record GovernanceTimelineHealthResponse(
		Instant checkedAt,
		GovernanceTimelineHealthStatus status,
		GovernanceTimelineResilienceMode resilienceMode,
		boolean partialTimelineSupported,
		boolean failOpenReadOnly,
		boolean streamingCompatible,
		List<String> degradedReasonTaxonomy,
		String message
) {
}
