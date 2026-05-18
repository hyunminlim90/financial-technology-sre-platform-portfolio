package com.fintech.sre.agent.knowledge.qdrant;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.knowledge.qdrant")
public record QdrantProperties(
		boolean enabled,
		String baseUrl,
		String collection,
		String apiKey,
		Duration timeout,
		int limit,
		int maxRetries
) {
	public Duration timeoutOrDefault() {
		return timeout == null ? Duration.ofMillis(800) : timeout;
	}

	public int limitOrDefault() {
		return limit <= 0 ? 10 : limit;
	}

	public int maxRetriesOrDefault() {
		return Math.max(maxRetries, 0);
	}
}
