package com.fintech.sre.agent.explanation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class ExplanationConfigurationTest {

	@Test
	void defaultProviderShouldUseStubExplanationAdapter() {
		try (AnnotationConfigApplicationContext context = context(Map.of(
				"agent.explanation.base-url", "https://api.openai.com/v1",
				"agent.explanation.model", "gpt-4o-mini"
		))) {
			assertThat(context.getBean(ExplanationPort.class))
					.isInstanceOf(StubExplanationAdapter.class);
		}
	}

	@Test
	void openAiProviderShouldEnableLlmExplanationAdapterOnly() {
		try (AnnotationConfigApplicationContext context = context(Map.of(
				"agent.explanation.provider", "openai",
				"agent.explanation.base-url", "https://api.openai.com/v1",
				"agent.explanation.api-key", "secret",
				"agent.explanation.model", "gpt-4o-mini"
		))) {
			assertThat(context.getBean(ExplanationPort.class))
					.isInstanceOf(LlmExplanationAdapter.class);
		}
	}

	private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("testProperties", properties));
		context.register(
				ExplanationWebClientConfig.class,
				StubExplanationAdapter.class,
				LlmExplanationAdapter.class
		);
		context.refresh();
		return context;
	}
}
