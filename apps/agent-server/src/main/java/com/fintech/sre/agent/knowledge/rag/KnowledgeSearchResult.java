package com.fintech.sre.agent.knowledge.rag;

import java.util.List;

public record KnowledgeSearchResult(
		List<KnowledgeDocument> documents
) {
	public boolean hasLayer(KnowledgeLayer layer) {
		return documents != null && documents.stream()
				.anyMatch(document -> document.layer() == layer);
	}

	public List<KnowledgeDocument> byLayer(KnowledgeLayer layer) {
		if (documents == null) {
			return List.of();
		}

		return documents.stream()
				.filter(document -> document.layer() == layer)
				.toList();
	}
}
