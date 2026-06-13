package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record LokiEvidenceQuery(
		EvidenceQuery evidenceQuery,
		String logSelector,
		LokiLogSemanticType semanticType
) {
	public LokiEvidenceQuery {
		Objects.requireNonNull(evidenceQuery, "evidenceQuery must not be null");
		Objects.requireNonNull(logSelector, "logSelector must not be null");
		Objects.requireNonNull(semanticType, "semanticType must not be null");

		if (evidenceQuery.sourceType() != EvidenceSourceType.LOGS) {
			throw new IllegalArgumentException(
					"Loki evidence query requires LOGS source"
			);
		}
		if (logSelector.isBlank()) {
			throw new IllegalArgumentException("logSelector must not be blank");
		}
	}

	public boolean logsEvidenceOnly() {
		return true;
	}

	public boolean exposesRawLogPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
