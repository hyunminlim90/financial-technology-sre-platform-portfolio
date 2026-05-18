package com.fintech.sre.agent.knowledge.vector.upsert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.embedding.EmbeddingVector;

class NoopVectorStoreUpsertClientTest {

	private final NoopVectorStoreUpsertClient client = new NoopVectorStoreUpsertClient();

	@Test
	void shouldReturnFailuresWhenVectorStoreIsNotConfigured() {
		EmbeddingVector vector = new EmbeddingVector(
				"chunk-1",
				"doc-1",
				List.of(0.1f, 0.2f),
				Map.of("type", "RUNBOOK")
		);

		VectorUpsertResult result = client.upsert(VectorUpsertRequest.of(List.of(vector))).block();

		assertThat(result.upsertedPointIds()).isEmpty();
		assertThat(result.failures()).hasSize(1);
		assertThat(result.failures().get(0).reasonCode())
				.isEqualTo("VECTOR_STORE_NOT_CONFIGURED");
	}
}
