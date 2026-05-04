package com.fintech.sre.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public record AgentProperties(
		Observability observability,
		Rag rag,
		Llm llm
) {

	public record Observability(
			String prometheusBaseUrl,
			String lokiBaseUrl,
			String jaegerBaseUrl
	) {
	}

	public record Rag(
			int topK,
			String knowledgeIndex
	) {
	}

	public record Llm(
			String model,
			double temperature
	) {
	}
}
