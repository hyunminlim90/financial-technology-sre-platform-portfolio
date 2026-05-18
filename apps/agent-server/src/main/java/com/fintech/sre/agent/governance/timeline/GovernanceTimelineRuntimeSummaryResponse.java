package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.List;

public record GovernanceTimelineRuntimeSummaryResponse(
		Instant checkedAt,
		GovernanceTimelineRuntimeMode runtimeMode,
		GovernanceTimelineHealthStatus healthStatus,
		GovernanceTimelineResilienceMode resilienceMode,
		boolean partialTimelineSupported,
		boolean failOpenReadOnly,
		boolean streamingCompatible,
		List<String> degradedSignals,
		String message
) {
}
