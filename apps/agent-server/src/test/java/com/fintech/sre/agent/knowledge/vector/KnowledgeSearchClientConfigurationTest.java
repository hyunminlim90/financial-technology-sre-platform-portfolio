package com.fintech.sre.agent.knowledge.vector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import com.fintech.sre.agent.knowledge.rag.KnowledgeSearchClient;
import com.fintech.sre.agent.knowledge.rag.StubKnowledgeSearchClient;

class KnowledgeSearchClientConfigurationTest {

	@Test
	void defaultModeShouldUseStubKnowledgeSearchClient() {
		try (AnnotationConfigApplicationContext context = context(Map.of())) {
			assertThat(context.getBean(KnowledgeSearchClient.class))
					.isInstanceOf(StubKnowledgeSearchClient.class);
		}
	}

	@Test
	void vectorModeShouldUseVectorKnowledgeSearchClientAndStubVectorAdapterByDefault() {
		try (AnnotationConfigApplicationContext context = context(Map.of(
				"agent.knowledge.search.client", "vector"
		))) {
			assertThat(context.getBean(KnowledgeSearchClient.class))
					.isInstanceOf(VectorKnowledgeSearchClient.class);
			assertThat(context.getBean(VectorSearchPort.class))
					.isInstanceOf(StubVectorSearchAdapter.class);
		}
	}

	private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("testProperties", properties));
		context.register(
				StubKnowledgeSearchClient.class,
				VectorKnowledgeSearchClient.class,
				StubVectorSearchAdapter.class,
				QdrantVectorSearchAdapter.class
		);
		context.refresh();
		return context;
	}
}
