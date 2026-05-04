package com.fintech.sre.agent.model.response;

import java.time.Instant;
import java.util.List;

public record PostmortemFrontMatter(
		String title,
		String knowledgeType,
		String domain,
		String failureMode,
		String severity,
		String environment,
		List<String> services,
		Instant incidentStart,
		Instant incidentEnd,
		Long durationMinutes,
		List<String> relatedScenarios,
		List<String> relatedRunbooks,
		List<String> relatedImprovements,
		List<String> relatedPreventiveDesigns,
		List<String> tags,
		String approvalStatus
) {
}
