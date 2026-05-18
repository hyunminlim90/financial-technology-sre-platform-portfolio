package com.fintech.sre.agent.knowledge.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

class EmbeddingServiceTest {

	@Test
	void providerFailureShouldBecomeEmbeddingFailures() {
		EmbeddingClient failingClient = requests -> Mono.error(new RuntimeException("provider down"));
		EmbeddingService service = new EmbeddingService(failingClient);

		EmbeddingRequest request = new EmbeddingRequest(
				"chunk-1",
				"doc-1",
				"content",
				Map.of("type", "RUNBOOK"),
				List.of("RUNBOOK")
		);

		EmbeddingResult result = service.embed(List.of(request)).block();

		assertThat(result.vectors()).isEmpty();
		assertThat(result.failures()).hasSize(1);
		assertThat(result.failures().get(0).reasonCode())
				.isEqualTo("EMBEDDING_PROVIDER_FAILURE");
	}
}
