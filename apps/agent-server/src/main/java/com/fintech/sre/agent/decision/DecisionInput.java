package com.fintech.sre.agent.decision;

import java.util.List;

import com.fintech.sre.agent.knowledge.layering.KnowledgeLayeringIssue;
import com.fintech.sre.agent.knowledge.rag.KnowledgeContext;
import com.fintech.sre.agent.model.common.IncidentContext;
import com.fintech.sre.agent.rag.RagSearchResult;

public record DecisionInput(
		IncidentContext incidentContext,
		RagSearchResult ragSearchResult,
		KnowledgeContext knowledgeContext,
		List<KnowledgeLayeringIssue> knowledgeLayeringIssues
) {
}
