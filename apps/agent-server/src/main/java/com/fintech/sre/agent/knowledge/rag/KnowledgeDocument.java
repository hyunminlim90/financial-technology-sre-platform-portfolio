package com.fintech.sre.agent.knowledge.rag;

import java.util.Map;

public record KnowledgeDocument(
		String id,
		KnowledgeLayer layer,
		String path,
		String title,
		String contentSnippet,
		Map<String, String> metadata
) {
}
