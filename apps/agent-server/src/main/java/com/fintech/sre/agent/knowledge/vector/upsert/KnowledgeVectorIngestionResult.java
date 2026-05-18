package com.fintech.sre.agent.knowledge.vector.upsert;

import com.fintech.sre.agent.knowledge.embedding.EmbeddingPreparationResult;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingResult;

public record KnowledgeVectorIngestionResult(
		EmbeddingPreparationResult preparationResult,
		EmbeddingResult embeddingResult,
		VectorUpsertResult upsertResult
) {
}
