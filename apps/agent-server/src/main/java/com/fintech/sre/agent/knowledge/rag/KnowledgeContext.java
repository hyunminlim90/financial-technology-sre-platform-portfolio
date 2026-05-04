package com.fintech.sre.agent.knowledge.rag;

import java.util.List;

public record KnowledgeContext(
		List<KnowledgeDocument> scenarios,
		List<KnowledgeDocument> runbooks,
		List<KnowledgeDocument> postmortems,
		List<KnowledgeDocument> improvements,
		List<KnowledgeDocument> preventiveDesigns,
		List<KnowledgeDocument> policies,
		List<KnowledgeDocument> ragDocs,
		List<KnowledgeDocument> protocols
) {
	public boolean hasScenario() {
		return scenarios != null && !scenarios.isEmpty();
	}

	public boolean hasRunbook() {
		return runbooks != null && !runbooks.isEmpty();
	}

	public boolean hasPolicy() {
		return policies != null && !policies.isEmpty();
	}

	public boolean onlyRagDocs() {
		return !hasScenario()
				&& !hasRunbook()
				&& isEmpty(postmortems)
				&& isEmpty(improvements)
				&& isEmpty(preventiveDesigns)
				&& isEmpty(policies)
				&& !isEmpty(ragDocs);
	}

	public String primaryScenarioId() {
		return hasScenario() ? scenarios.get(0).id() : null;
	}

	public String primaryRunbookId() {
		return hasRunbook() ? runbooks.get(0).id() : null;
	}

	private boolean isEmpty(List<?> values) {
		return values == null || values.isEmpty();
	}
}
