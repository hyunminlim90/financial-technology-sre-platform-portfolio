package com.fintech.sre.agent.knowledge.vector.qdrant;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(VectorQdrantProperties.class)
public class QdrantWebClientConfig {

	@Bean
	public WebClient qdrantWebClient(VectorQdrantProperties properties) {
		WebClient.Builder builder = WebClient.builder()
				.baseUrl(properties.baseUrl());

		if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
			builder.defaultHeader("api-key", properties.apiKey());
		}

		return builder.build();
	}
}
