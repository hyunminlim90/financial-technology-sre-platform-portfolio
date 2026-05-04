package com.fintech.sre.agent.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "observability")
public record ObservabilityProperties(
		Prometheus prometheus,
		Loki loki,
		Jaeger jaeger
) {
	public record Prometheus(String baseUrl) {
	}

	public record Loki(String baseUrl) {
	}

	public record Jaeger(String baseUrl) {
	}
}
