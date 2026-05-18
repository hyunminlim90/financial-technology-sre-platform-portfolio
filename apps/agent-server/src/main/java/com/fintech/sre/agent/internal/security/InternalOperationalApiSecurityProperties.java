package com.fintech.sre.agent.internal.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.internal.security")
public record InternalOperationalApiSecurityProperties(
		boolean enabled,
		boolean requireHeader,
		String headerName,
		String headerValue,
		List<String> protectedPaths
) {
	public String headerNameOrDefault() {
		return headerName == null || headerName.isBlank()
				? "X-FIN-SRE-INTERNAL"
				: headerName;
	}

	public boolean hasSecret() {
		return headerValue != null && !headerValue.isBlank();
	}

	public List<String> protectedPathsOrDefault() {
		if (protectedPaths == null || protectedPaths.isEmpty()) {
			return List.of(
					"/internal/admin/",
					"/internal/alerts/",
					"/internal/recommendations/",
					"/internal/incidents/",
					"/internal/execution-plans/",
					"/internal/execution-results/",
					"/internal/verification-results/",
					"/internal/postmortem-drafts/",
					"/internal/learning-candidates/",
					"/internal/knowledge-promotion-plans/",
					"/internal/knowledge-updates/",
					"/internal/governance/"
			);
		}

		return protectedPaths.stream()
				.filter(path -> path != null && !path.isBlank())
				.map(this::normalizePathPrefix)
				.toList();
	}

	private String normalizePathPrefix(String path) {
		String trimmed = path.trim();

		if (!trimmed.startsWith("/")) {
			trimmed = "/" + trimmed;
		}

		if (!trimmed.endsWith("/")) {
			trimmed = trimmed + "/";
		}

		return trimmed;
	}
}
