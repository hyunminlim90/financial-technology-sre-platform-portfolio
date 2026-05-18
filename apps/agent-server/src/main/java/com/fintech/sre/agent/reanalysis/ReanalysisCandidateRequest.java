package com.fintech.sre.agent.reanalysis;

import java.util.Map;

public record ReanalysisCandidateRequest(
		String sourceVerificationResultId,
		String sourceExecutionResultId,
		ReanalysisTriggerReason reason,
		String operatorId,
		String summary,
		Map<String, String> metadata
) {
}
