package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record PrometheusEvidenceQuery(
		EvidenceQuery evidenceQuery,
		String metricName,
		PrometheusMetricSemanticType semanticType
) {
	public PrometheusEvidenceQuery {
		Objects.requireNonNull(evidenceQuery, "evidenceQuery must not be null");
		Objects.requireNonNull(metricName, "metricName must not be null");
		Objects.requireNonNull(semanticType, "semanticType must not be null");

		if (evidenceQuery.sourceType() != EvidenceSourceType.METRICS) {
			throw new IllegalArgumentException(
					"Prometheus evidence query requires METRICS source"
			);
		}
		if (metricName.isBlank()) {
			throw new IllegalArgumentException("metricName must not be blank");
		}
	}

	public boolean metricsEvidenceOnly() {
		return true;
	}

	public boolean exposesRawPrometheusPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
