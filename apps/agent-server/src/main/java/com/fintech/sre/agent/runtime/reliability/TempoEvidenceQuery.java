package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record TempoEvidenceQuery(
		EvidenceQuery evidenceQuery,
		String traceSelector,
		TempoTraceSemanticType semanticType
) {
	public TempoEvidenceQuery {
		Objects.requireNonNull(evidenceQuery, "evidenceQuery must not be null");
		Objects.requireNonNull(traceSelector, "traceSelector must not be null");
		Objects.requireNonNull(semanticType, "semanticType must not be null");

		if (evidenceQuery.sourceType() != EvidenceSourceType.TRACES) {
			throw new IllegalArgumentException(
					"Tempo evidence query requires TRACES source"
			);
		}
		if (traceSelector.isBlank()) {
			throw new IllegalArgumentException("traceSelector must not be blank");
		}
	}

	public boolean tracesEvidenceOnly() {
		return true;
	}

	public boolean exposesRawTracePayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
