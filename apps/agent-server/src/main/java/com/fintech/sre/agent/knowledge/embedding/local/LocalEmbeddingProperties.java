package com.fintech.sre.agent.knowledge.embedding.local;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.knowledge.embedding.local")
public record LocalEmbeddingProperties(
		boolean enabled,
		String baseUrl,
		String model,
		Duration timeout,
		int maxRetries
) {
	public Duration timeoutOrDefault() {
		return timeout == null ? Duration.ofSeconds(2) : timeout;
	}

	public int maxRetriesOrDefault() {
		return Math.max(maxRetries, 0);
	}

	public String modelOrDefault() {
		return model == null || model.isBlank() ? "bge-m3" : model;
	}
}
