package com.fintech.sre.agent.knowledge.vector;

import java.util.Map;

import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;

public record VectorSearchDocument(
		String id,
		KnowledgeLayer layer,
		String path,
		String title,
		String contentSnippet,
		double score,
		Map<String, String> metadata
) {
}
