package com.fintech.sre.agent.knowledge.embedding;

import java.util.List;

public record EmbeddingResult(
		List<EmbeddingVector> vectors,
		List<EmbeddingFailure> failures
) {
	public EmbeddingResult {
		vectors = vectors == null ? List.of() : List.copyOf(vectors);
		failures = failures == null ? List.of() : List.copyOf(failures);
	}

	public static EmbeddingResult empty() {
		return new EmbeddingResult(List.of(), List.of());
	}

	public static EmbeddingResult success(List<EmbeddingVector> vectors) {
		return new EmbeddingResult(
				vectors == null ? List.of() : List.copyOf(vectors),
				List.of()
		);
	}

	public static EmbeddingResult failed(List<EmbeddingFailure> failures) {
		return new EmbeddingResult(
				List.of(),
				failures == null ? List.of() : List.copyOf(failures)
		);
	}

	public boolean hasVectors() {
		return vectors != null && !vectors.isEmpty();
	}
}
