package com.fintech.sre.agent.alert.dedup;

public record AlertDeduplicationKey(
		String alertName,
		String service,
		String status
) {
	public static AlertDeduplicationKey of(
			String alertName,
			String service,
			String status
	) {
		return new AlertDeduplicationKey(
				normalize(alertName),
				normalize(service),
				normalize(status)
		);
	}

	private static String normalize(String value) {
		return value == null || value.isBlank()
				? "unknown"
				: value.trim().toLowerCase();
	}
}
