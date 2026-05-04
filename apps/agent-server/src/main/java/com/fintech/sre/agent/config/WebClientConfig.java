package com.fintech.sre.agent.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.fintech.sre.agent.observability.config.ObservabilityProperties;

@Configuration
public class WebClientConfig {

	@Bean
	WebClient webClient() {
		return WebClient.builder().build();
	}

	@Bean
	ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return objectMapper;
	}

	@Bean
	@Qualifier("prometheusWebClient")
	WebClient prometheusWebClient(ObservabilityProperties properties) {
		return WebClient.builder()
				.baseUrl(properties.prometheus().baseUrl())
				.build();
	}

	@Bean
	@Qualifier("lokiWebClient")
	WebClient lokiWebClient(ObservabilityProperties properties) {
		return WebClient.builder()
				.baseUrl(properties.loki().baseUrl())
				.build();
	}

	@Bean
	@Qualifier("jaegerWebClient")
	WebClient jaegerWebClient(ObservabilityProperties properties) {
		return WebClient.builder()
				.baseUrl(properties.jaeger().baseUrl())
				.build();
	}
}
