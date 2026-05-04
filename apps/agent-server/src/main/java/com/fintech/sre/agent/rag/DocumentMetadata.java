package com.fintech.sre.agent.rag;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DocumentMetadata(
		String title,
		KnowledgeType knowledgeType,
		String domain,
		String failureMode,
		String environment,
		String severity,
		String impactScope,
		List<String> services,
		List<String> tags,
		List<String> relatedScenarios,
		List<String> relatedRunbooks,
		List<String> relatedPostmortems,
		List<String> relatedImprovements,
		List<String> relatedPreventiveDesigns,
		Instant createdAt,
		Instant updatedAt,
		Map<String, String> raw
) {
}
