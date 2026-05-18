package com.fintech.sre.agent.alert.dedup;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.alert.deduplication")
public record AlertDeduplicationProperties(
		boolean enabled,
		Duration window
) {
	public Duration windowOrDefault() {
		return window == null ? Duration.ofMinutes(5) : window;
	}
}
