package com.fintech.sre.agent.governance.dashboard;

public record GovernanceRiskIndicator(
		String name,
		GovernanceRiskLevel level,
		double value,
		double threshold,
		String reason
) {
}
