package com.fintech.sre.agent.alert;

public enum AlertSeverity {
	INFO,
	WARNING,
	CRITICAL,
	UNKNOWN;

	public static AlertSeverity from(String value) {
		if (value == null || value.isBlank()) {
			return UNKNOWN;
		}

		return switch (value.toLowerCase()) {
			case "info" -> INFO;
			case "warning", "warn" -> WARNING;
			case "critical", "page", "high" -> CRITICAL;
			default -> UNKNOWN;
		};
	}
}
