package com.fintech.sre.agent.knowledge.vector.qdrant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.knowledge.qdrant")
public record VectorQdrantProperties(
		String baseUrl,
		String collection,
		String apiKey,
		int limit,
		double scoreThreshold
) {
}
