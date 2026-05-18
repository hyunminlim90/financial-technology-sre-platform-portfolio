package com.fintech.sre.agent.knowledge.embedding;

public record EmbeddingFailure(
		String chunkId,
		String reasonCode,
		String reason
) {
}
