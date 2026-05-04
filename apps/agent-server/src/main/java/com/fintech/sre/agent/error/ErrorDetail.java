package com.fintech.sre.agent.error;

public record ErrorDetail(
		String code,
		ErrorSeverity severity,
		String message
) {
}
