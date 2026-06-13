package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record LokiEvidenceMapping(
		String logSelector,
		LokiLogSemanticType semanticType,
		String normalizedSignalId,
		String normalizedSummary,
		boolean sanitizedConsistencyMetadataPresent,
		boolean customerPayloadExposed,
		boolean tokenExposed,
		boolean secretExposed,
		boolean internalIpExposed,
		boolean highCardinalityLabelsPresent,
		LokiEvidenceRejectionReason rejectionReason
) {
	public LokiEvidenceMapping {
		Objects.requireNonNull(logSelector, "logSelector must not be null");
		Objects.requireNonNull(semanticType, "semanticType must not be null");
		Objects.requireNonNull(
				normalizedSignalId,
				"normalizedSignalId must not be null"
		);
		Objects.requireNonNull(
				normalizedSummary,
				"normalizedSummary must not be null"
		);

		if (logSelector.isBlank()) {
			throw new IllegalArgumentException("logSelector must not be blank");
		}
		if (normalizedSignalId.isBlank()) {
			throw new IllegalArgumentException("normalizedSignalId must not be blank");
		}
		if (normalizedSummary.isBlank()) {
			throw new IllegalArgumentException("normalizedSummary must not be blank");
		}
	}

	public EvidenceSignal toEvidenceSignal() {
		if (containsSensitivePayload()) {
			throw new IllegalStateException(
					"sensitive log payload must not be exposed as semantic evidence"
			);
		}
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
		return switch (semanticType) {
			case PAYMENT_CONSISTENCY_EVENT ->
					sanitizedConsistencyMetadataPresent
							? EvidenceSignalType.PAYMENT_SAFETY
							: EvidenceSignalType.LOG;
			case VERIFICATION_EVENT -> EvidenceSignalType.VERIFICATION;
			default -> EvidenceSignalType.LOG;
		};
	}

	public boolean normalizedEvidenceOnly() {
		return true;
	}

	public boolean exposesRawLogPayload() {
		return false;
	}

	public boolean suppressesSensitivePayload() {
		return !containsSensitivePayload();
	}

	public boolean suppressesHighCardinalityLabels() {
		return !highCardinalityLabelsPresent;
	}

	public boolean paymentSafetyElevated() {
		return evidenceSignalType() == EvidenceSignalType.PAYMENT_SAFETY;
	}

	private boolean containsSensitivePayload() {
		return customerPayloadExposed
				|| tokenExposed
				|| secretExposed
				|| internalIpExposed;
	}
}
