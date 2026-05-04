package com.fintech.sre.agent.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class EmbeddingConfigurationTest {

	@Test
	void defaultProviderShouldUseStubEmbeddingAdapter() {
		try (AnnotationConfigApplicationContext context = context(Map.of(
				"agent.embedding.base-url", "http://localhost:11434",
				"agent.embedding.model", "stub-384",
				"agent.embedding.dimensions", 384
		))) {
			assertThat(context.getBean(EmbeddingPort.class))
					.isInstanceOf(StubEmbeddingAdapter.class);
		}
	}

	@Test
	void openAiAdapterShouldBeEnabledOnlyWhenConfigured() {
		try (AnnotationConfigApplicationContext context = context(Map.of(
				"agent.embedding.provider", "openai",
				"agent.embedding.base-url", "https://api.openai.com/v1",
				"agent.embedding.api-key", "secret",
				"agent.embedding.model", "text-embedding-3-small",
				"agent.embedding.dimensions", 1536
		))) {
			assertThat(context.getBean(EmbeddingPort.class))
					.isInstanceOf(OpenAiEmbeddingAdapter.class);
		}
	}

	private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("testProperties", properties));
		context.register(
				EmbeddingWebClientConfig.class,
				StubEmbeddingAdapter.class,
				OpenAiEmbeddingAdapter.class
		);
		context.refresh();
		return context;
	}
}
