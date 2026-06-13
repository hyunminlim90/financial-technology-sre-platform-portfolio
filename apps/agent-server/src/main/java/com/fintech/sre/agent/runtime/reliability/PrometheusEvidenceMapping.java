package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record PrometheusEvidenceMapping(
		String metricName,
		PrometheusMetricSemanticType semanticType,
		String normalizedSignalId,
		String normalizedSummary,
		boolean paymentConsistencyMetadataPresent,
		boolean highCardinalityLabelsPresent,
		PrometheusEvidenceRejectionReason rejectionReason
) {
	public PrometheusEvidenceMapping {
		Objects.requireNonNull(metricName, "metricName must not be null");
		Objects.requireNonNull(semanticType, "semanticType must not be null");
		Objects.requireNonNull(
				normalizedSignalId,
				"normalizedSignalId must not be null"
		);
		Objects.requireNonNull(
				normalizedSummary,
				"normalizedSummary must not be null"
		);

		if (metricName.isBlank()) {
			throw new IllegalArgumentException("metricName must not be blank");
		}
		if (normalizedSignalId.isBlank()) {
			throw new IllegalArgumentException("normalizedSignalId must not be blank");
		}
		if (normalizedSummary.isBlank()) {
			throw new IllegalArgumentException("normalizedSummary must not be blank");
		}
	}

	public EvidenceSignal toEvidenceSignal() {
		if (highCardinalityLabelsPresent) {
			throw new IllegalStateException(
					"high-cardinality labels must not be exposed as semantic evidence"
			);
		}
		return new EvidenceSignal(
				evidenceSignalType(),
				normalizedSignalId,
				normalizedSummary
		);
	}

	public EvidenceSignalType evidenceSignalType() {
		if (semanticType == PrometheusMetricSemanticType.PAYMENT_CONSISTENCY
				&& paymentConsistencyMetadataPresent) {
			return EvidenceSignalType.PAYMENT_SAFETY;
		}
		return EvidenceSignalType.METRIC;
	}

	public boolean normalizedEvidenceOnly() {
		return true;
	}

	public boolean exposesRawPrometheusPayload() {
		return false;
	}

	public boolean suppressesHighCardinalityLabels() {
		return !highCardinalityLabelsPresent;
	}

	public boolean paymentSafetyElevated() {
		return evidenceSignalType() == EvidenceSignalType.PAYMENT_SAFETY;
	}
}
