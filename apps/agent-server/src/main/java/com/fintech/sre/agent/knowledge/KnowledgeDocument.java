package com.fintech.sre.agent.knowledge;

import java.util.List;
import java.util.Map;

public record KnowledgeDocument(
		String id,
		KnowledgeDocumentType type,
		String title,
		String path,
		String domain,
		String service,
		List<String> scenarioIds,
		List<String> runbookIds,
		List<String> evidenceCodes,
		List<String> actionTypes,
		double score,
		Map<String, String> metadata,
		String summary
) {
	public KnowledgeDocument {
		scenarioIds = scenarioIds == null ? List.of() : List.copyOf(scenarioIds);
		runbookIds = runbookIds == null ? List.of() : List.copyOf(runbookIds);
		evidenceCodes = evidenceCodes == null ? List.of() : List.copyOf(evidenceCodes);
		actionTypes = actionTypes == null ? List.of() : List.copyOf(actionTypes);
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
