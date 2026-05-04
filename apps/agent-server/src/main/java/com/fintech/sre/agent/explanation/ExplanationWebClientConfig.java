package com.fintech.sre.agent.explanation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(ExplanationProperties.class)
public class ExplanationWebClientConfig {

	@Bean
	public WebClient explanationWebClient(ExplanationProperties properties) {
		WebClient.Builder builder = WebClient.builder()
				.baseUrl(properties.baseUrl());

		if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
			builder.defaultHeader("Authorization", "Bearer " + properties.apiKey());
		}

		return builder.build();
	}
}
