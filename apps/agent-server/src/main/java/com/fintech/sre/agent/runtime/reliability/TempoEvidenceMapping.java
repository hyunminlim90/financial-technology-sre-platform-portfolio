package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record TempoEvidenceMapping(
		String traceSelector,
		TempoTraceSemanticType semanticType,
		String normalizedSignalId,
		String normalizedSummary,
		boolean sanitizedConsistencyMetadataPresent,
		boolean customerPayloadExposed,
		boolean tokenExposed,
		boolean secretExposed,
		boolean internalIpExposed,
		boolean highCardinalityIdentifierPresent,
		TempoEvidenceRejectionReason rejectionReason
) {
	public TempoEvidenceMapping {
		Objects.requireNonNull(traceSelector, "traceSelector must not be null");
		Objects.requireNonNull(semanticType, "semanticType must not be null");
		Objects.requireNonNull(
				normalizedSignalId,
				"normalizedSignalId must not be null"
		);
		Objects.requireNonNull(
				normalizedSummary,
				"normalizedSummary must not be null"
		);

		if (traceSelector.isBlank()) {
			throw new IllegalArgumentException("traceSelector must not be blank");
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
					"sensitive trace payload must not be exposed as semantic evidence"
			);
		}
		if (highCardinalityIdentifierPresent) {
			throw new IllegalStateException(
					"high-cardinality trace identifiers must not be exposed as semantic evidence"
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
			case PAYMENT_CONSISTENCY_TRACE ->
					sanitizedConsistencyMetadataPresent
							? EvidenceSignalType.PAYMENT_SAFETY
							: EvidenceSignalType.TRACE;
			case VERIFICATION_TRACE -> EvidenceSignalType.VERIFICATION;
			default -> EvidenceSignalType.TRACE;
		};
	}

	public boolean normalizedEvidenceOnly() {
		return true;
	}

	public boolean exposesRawTracePayload() {
		return false;
	}

	public boolean suppressesSensitivePayload() {
		return !containsSensitivePayload();
	}

	public boolean suppressesHighCardinalityIdentifiers() {
		return !highCardinalityIdentifierPresent;
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
