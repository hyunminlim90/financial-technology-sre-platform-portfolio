package com.fintech.sre.agent.knowledge.layering;

import java.util.List;

import com.fintech.sre.agent.knowledge.rag.KnowledgeContext;
import com.fintech.sre.agent.knowledge.rag.KnowledgeDocument;

public class KnowledgePriorityResolver {

	public List<KnowledgeDocument> resolvePriority(KnowledgeContext context) {
		if (context == null) {
			return List.of();
		}

		return java.util.stream.Stream.of(
						nullSafe(context.preventiveDesigns()),
						nullSafe(context.improvements()),
						nullSafe(context.postmortems()),
						nullSafe(context.runbooks()),
						nullSafe(context.scenarios()),
						nullSafe(context.ragDocs())
				)
				.flatMap(List::stream)
				.toList();
	}

	private List<KnowledgeDocument> nullSafe(List<KnowledgeDocument> documents) {
		return documents == null ? List.of() : documents;
	}
}
