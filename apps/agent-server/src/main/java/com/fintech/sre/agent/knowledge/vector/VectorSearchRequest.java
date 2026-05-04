package com.fintech.sre.agent.knowledge.vector;

import java.util.List;
import java.util.Map;

import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;

public record VectorSearchRequest(
		String query,
		List<KnowledgeLayer> layers,
		Map<String, String> filters,
		int limit
) {
}
