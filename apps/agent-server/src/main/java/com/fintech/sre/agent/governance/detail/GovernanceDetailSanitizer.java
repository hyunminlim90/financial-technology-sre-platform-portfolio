package com.fintech.sre.agent.governance.detail;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class GovernanceDetailSanitizer {

	private static final List<String> FORBIDDEN_KEYWORDS = List.of(
			"payload",
			"customer",
			"secret",
			"token",
			"password",
			"payment",
			"rawlog",
			"prompt"
	);

	public String safeText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		if (containsForbiddenKeyword(value)) {
			return "[redacted]";
		}
		return value;
	}

	public String safeStatus(Object status) {
		return status == null ? "UNKNOWN" : status.toString();
	}

	public List<String> safeTexts(List<String> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return values.stream()
				.map(this::safeText)
				.filter(value -> value != null && !value.isBlank())
				.toList();
	}

	private boolean containsForbiddenKeyword(String value) {
		String lower = value.toLowerCase(Locale.ROOT);
		return FORBIDDEN_KEYWORDS.stream().anyMatch(lower::contains);
	}
}
