package com.fintech.sre.agent.knowledge.chunk;

import java.util.List;
import java.util.Map;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;

public record KnowledgeChunk(
		String id,
		String documentId,
		KnowledgeDocumentType documentType,
		String title,
		String path,
		String domain,
		String service,
		int chunkIndex,
		String content,
		String summary,
		List<String> scenarioIds,
		List<String> runbookIds,
		List<String> postmortemIds,
		List<String> improvementIds,
		List<String> preventiveDesignIds,
		List<String> policyIds,
		List<String> evidenceCodes,
		List<String> actionTypes,
		Map<String, String> metadata
) {
	public KnowledgeChunk {
		scenarioIds = scenarioIds == null ? List.of() : List.copyOf(scenarioIds);
		runbookIds = runbookIds == null ? List.of() : List.copyOf(runbookIds);
		postmortemIds = postmortemIds == null ? List.of() : List.copyOf(postmortemIds);
		improvementIds = improvementIds == null ? List.of() : List.copyOf(improvementIds);
		preventiveDesignIds = preventiveDesignIds == null ? List.of() : List.copyOf(preventiveDesignIds);
		policyIds = policyIds == null ? List.of() : List.copyOf(policyIds);
		evidenceCodes = evidenceCodes == null ? List.of() : List.copyOf(evidenceCodes);
		actionTypes = actionTypes == null ? List.of() : List.copyOf(actionTypes);
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
