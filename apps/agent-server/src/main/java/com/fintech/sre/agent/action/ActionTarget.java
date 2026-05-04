package com.fintech.sre.agent.action;

public record ActionTarget(
		String domain,
		String service,
		String resourceType,
		String resourceName,
		String environment
) {
}
