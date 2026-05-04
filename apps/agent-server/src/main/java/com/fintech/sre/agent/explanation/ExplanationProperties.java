package com.fintech.sre.agent.explanation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.explanation")
public record ExplanationProperties(
		String provider,
		String baseUrl,
		String apiKey,
		String model
) {
}
