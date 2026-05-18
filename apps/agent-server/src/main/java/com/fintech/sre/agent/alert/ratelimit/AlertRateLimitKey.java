package com.fintech.sre.agent.alert.ratelimit;

public record AlertRateLimitKey(
		String service,
		String severity
) {
	public static AlertRateLimitKey of(String service, String severity) {
		return new AlertRateLimitKey(
				normalize(service),
				normalize(severity)
		);
	}

	private static String normalize(String value) {
		return value == null || value.isBlank()
				? "unknown"
				: value.trim().toLowerCase();
	}
}
