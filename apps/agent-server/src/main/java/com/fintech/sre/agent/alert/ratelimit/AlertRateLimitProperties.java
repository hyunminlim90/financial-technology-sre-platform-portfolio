package com.fintech.sre.agent.alert.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.alert.rate-limit")
public record AlertRateLimitProperties(
		boolean enabled,
		Duration window,
		long maxRecommendationsPerWindow
) {
	public Duration windowOrDefault() {
		return window == null ? Duration.ofMinutes(1) : window;
	}

	public long maxRecommendationsPerWindowOrDefault() {
		return maxRecommendationsPerWindow <= 0 ? 10 : maxRecommendationsPerWindow;
	}
}
