package com.fintech.sre.agent.reanalysis;

import java.time.Instant;
import java.util.Map;

public record ReanalysisTriggerCandidate(
		String reanalysisCandidateId,
		String incidentId,
		String sourceVerificationResultId,
		String sourceExecutionResultId,
		ReanalysisTriggerReason reason,
		ReanalysisCandidateStatus status,
		String operatorId,
		String summary,
		Instant createdAt,
		Map<String, String> metadata
) {
	public ReanalysisTriggerCandidate {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
