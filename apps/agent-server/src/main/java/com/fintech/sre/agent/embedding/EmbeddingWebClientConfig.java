package com.fintech.sre.agent.embedding;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingWebClientConfig {

	@Bean
	public WebClient embeddingWebClient(EmbeddingProperties properties) {
		WebClient.Builder builder = WebClient.builder()
				.baseUrl(properties.baseUrl());

		if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
			builder.defaultHeader("Authorization", "Bearer " + properties.apiKey());
		}

		return builder.build();
	}
}
