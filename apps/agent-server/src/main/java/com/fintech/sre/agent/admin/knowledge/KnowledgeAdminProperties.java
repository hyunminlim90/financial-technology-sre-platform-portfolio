package com.fintech.sre.agent.admin.knowledge;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.admin.knowledge")
public record KnowledgeAdminProperties(
		boolean enabled,
		List<String> allowedRootPaths
) {
	public boolean enabledOrDefault() {
		return enabled;
	}

	public List<String> allowedRootPathsOrDefault() {
		return allowedRootPaths == null ? List.of() : List.copyOf(allowedRootPaths);
	}
}
