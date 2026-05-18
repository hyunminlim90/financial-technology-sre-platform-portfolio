package com.fintech.sre.agent.knowledge.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StubEmbeddingClientTest {

	private final StubEmbeddingClient client = new StubEmbeddingClient();

	@Test
	void shouldReturnDeterministicVector() {
		EmbeddingRequest request = new EmbeddingRequest(
				"chunk-1",
				"doc-1",
				"same content",
				Map.of("type", "RUNBOOK"),
				List.of("RUNBOOK")
		);

		EmbeddingResult first = client.embed(List.of(request)).block();
		EmbeddingResult second = client.embed(List.of(request)).block();

		assertThat(first.vectors()).hasSize(1);
		assertThat(first.vectors().get(0).vector())
				.isEqualTo(second.vectors().get(0).vector());
	}
}
