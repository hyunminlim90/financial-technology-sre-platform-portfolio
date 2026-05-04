package com.fintech.sre.agent.knowledge.vector;

import java.util.List;

public record VectorSearchResult(
		List<VectorSearchDocument> documents
) {
}
