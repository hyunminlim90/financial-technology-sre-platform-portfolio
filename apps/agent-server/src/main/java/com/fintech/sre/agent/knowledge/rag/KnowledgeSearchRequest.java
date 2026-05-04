package com.fintech.sre.agent.knowledge.rag;

import java.util.List;
import java.util.Map;

public record KnowledgeSearchRequest(
		String query,
		List<KnowledgeLayer> requiredLayers,
		List<KnowledgeLayer> optionalLayers,
		Map<String, String> filters,
		int limit
) {
}
