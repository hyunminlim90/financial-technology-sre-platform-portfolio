package com.fintech.sre.agent.knowledge;

import java.util.List;

import com.fintech.sre.agent.evidence.EvidenceContext;

public record KnowledgeSearchQuery(
		String incidentId,
		String service,
		String domain,
		String severity,
		List<String> scenarioIds,
		List<String> runbookIds,
		List<String> evidenceCodes,
		EvidenceContext evidenceContext,
		int limit
) {
	public KnowledgeSearchQuery {
		scenarioIds = scenarioIds == null ? List.of() : List.copyOf(scenarioIds);
		runbookIds = runbookIds == null ? List.of() : List.copyOf(runbookIds);
		evidenceCodes = evidenceCodes == null ? List.of() : List.copyOf(evidenceCodes);
	}

	public static KnowledgeSearchQuery from(EvidenceContext evidenceContext, String service, String domain) {
		return new KnowledgeSearchQuery(
				evidenceContext == null ? "unknown" : evidenceContext.incidentId(),
				service,
				domain,
				null,
				evidenceContext == null ? List.of() : evidenceContext.matchedScenarioIds(),
				evidenceContext == null ? List.of() : evidenceContext.matchedRunbookIds(),
				evidenceContext == null ? List.of() : evidenceContext.signals().stream()
						.map(signal -> signal.code())
						.toList(),
				evidenceContext,
				10
		);
	}
}
