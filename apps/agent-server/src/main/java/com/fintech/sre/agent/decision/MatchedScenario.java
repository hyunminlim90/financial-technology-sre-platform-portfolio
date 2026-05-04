package com.fintech.sre.agent.decision;

import com.fintech.sre.agent.model.common.ImpactScope;
import com.fintech.sre.agent.model.common.Severity;

public record MatchedScenario(
		String failureMode,
		String domain,
		String title,
		String path,
		Severity severity,
		ImpactScope impactScope
) {
}
