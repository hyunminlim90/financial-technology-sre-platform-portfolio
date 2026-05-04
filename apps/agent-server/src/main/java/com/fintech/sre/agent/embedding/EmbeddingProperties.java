package com.fintech.sre.agent.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.embedding")
public record EmbeddingProperties(
		String provider,
		String baseUrl,
		String apiKey,
		String model,
		int dimensions
) {
}
