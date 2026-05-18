package com.fintech.sre.agent.knowledge.vector.upsert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.embedding.EmbeddingVector;

class QdrantPointMapperTest {

	private final QdrantPointMapper mapper = new QdrantPointMapper();

	@Test
	void shouldMapEmbeddingVectorToQdrantPoint() {
		EmbeddingVector vector = new EmbeddingVector(
				"chunk-1",
				"doc-1",
				List.of(0.1f, 0.2f),
				Map.of("type", "RUNBOOK")
		);

		QdrantUpsertRequest.Point point = mapper.toPoint(vector);

		assertThat(point.id()).isEqualTo("chunk-1");
		assertThat(point.vector()).containsExactly(0.1f, 0.2f);
		assertThat(point.payload()).containsEntry("type", "RUNBOOK");
	}
}
