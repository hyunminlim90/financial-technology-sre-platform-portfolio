package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceCollectionRequest(
		List<EvidenceAdapterPort> adapters,
		List<EvidenceQuery> queries
) {
	public EvidenceCollectionRequest {
		Objects.requireNonNull(adapters, "adapters must not be null");
		Objects.requireNonNull(queries, "queries must not be null");
		adapters = List.copyOf(adapters);
		queries = List.copyOf(queries);
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
