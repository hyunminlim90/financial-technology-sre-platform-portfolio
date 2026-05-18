package com.fintech.sre.agent.knowledge.embedding;

import java.util.List;

public record EmbeddingPreparationResult(
		List<EmbeddingRequest> requests,
		List<String> rejectedChunkIds,
		List<String> errors
) {
	public EmbeddingPreparationResult {
		requests = requests == null ? List.of() : List.copyOf(requests);
		rejectedChunkIds = rejectedChunkIds == null ? List.of() : List.copyOf(rejectedChunkIds);
		errors = errors == null ? List.of() : List.copyOf(errors);
	}
}
