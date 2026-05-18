package com.fintech.sre.agent.reanalysis;

public record ReanalysisCandidateResponse(
		String reanalysisCandidateId,
		String incidentId,
		ReanalysisTriggerReason reason,
		ReanalysisCandidateStatus status,
		String operatorId,
		String summary
) {
}
