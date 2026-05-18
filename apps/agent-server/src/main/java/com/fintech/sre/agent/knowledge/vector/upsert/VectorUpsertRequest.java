package com.fintech.sre.agent.knowledge.vector.upsert;

import java.util.List;

import com.fintech.sre.agent.knowledge.embedding.EmbeddingVector;

public record VectorUpsertRequest(
		List<EmbeddingVector> vectors
) {
	public VectorUpsertRequest {
		vectors = vectors == null ? List.of() : List.copyOf(vectors);
	}

	public static VectorUpsertRequest of(List<EmbeddingVector> vectors) {
		return new VectorUpsertRequest(vectors == null ? List.of() : List.copyOf(vectors));
	}

	public boolean isEmpty() {
		return vectors == null || vectors.isEmpty();
	}
}
