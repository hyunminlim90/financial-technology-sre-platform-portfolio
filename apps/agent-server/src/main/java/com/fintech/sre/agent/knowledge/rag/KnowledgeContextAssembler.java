package com.fintech.sre.agent.knowledge.rag;

import org.springframework.stereotype.Component;

@Component
public class KnowledgeContextAssembler {

	public KnowledgeContext assemble(KnowledgeSearchResult result) {
		return new KnowledgeContext(
				result.byLayer(KnowledgeLayer.SCENARIO),
				result.byLayer(KnowledgeLayer.RUNBOOK),
				result.byLayer(KnowledgeLayer.POSTMORTEM),
				result.byLayer(KnowledgeLayer.IMPROVEMENT),
				result.byLayer(KnowledgeLayer.PREVENTIVE_DESIGN),
				result.byLayer(KnowledgeLayer.POLICY),
				result.byLayer(KnowledgeLayer.RAG_DOC),
				result.byLayer(KnowledgeLayer.PROTOCOL)
		);
	}
}
