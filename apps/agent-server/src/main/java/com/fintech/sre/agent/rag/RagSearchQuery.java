package com.fintech.sre.agent.rag;

import java.util.List;
import java.util.Map;

import com.fintech.sre.agent.model.common.IncidentContext;

public record RagSearchQuery(
		String incidentId,
		String alertName,
		String service,
		String environment,
		String domainHint,
		String failureModeHint,
		String severityHint,
		String impactScopeHint,
		List<String> keywords,
		List<KnowledgeType> targetKnowledgeTypes,
		Map<String, Object> metricHints,
		int topK
) {

	public static RagSearchQuery from(IncidentContext context) {
		return new RagSearchQuery(
				context.incidentId(),
				context.alertName(),
				context.service(),
				context.environment(),
				context.domainHint(),
				context.failureModeHint(),
				context.severityHint(),
				context.impactScopeHint(),
				context.keywords(),
				List.of(
						KnowledgeType.PROTOCOL,
						KnowledgeType.SCENARIO,
						KnowledgeType.RUNBOOK,
						KnowledgeType.IMPROVEMENT,
						KnowledgeType.PREVENTIVE_DESIGN,
						KnowledgeType.POSTMORTEM,
						KnowledgeType.RAG_DOC
				),
				context.metricHints(),
				20
		);
	}
}
