package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceGovernancePolicy(
		EvidenceTrustLevel trustLevel,
		EvidenceIntegrityStatus integrityStatus,
		EvidenceClassification classification,
		EvidenceProvenance provenance
) {
	public EvidenceGovernancePolicy {
		Objects.requireNonNull(trustLevel, "trustLevel must not be null");
		Objects.requireNonNull(
				integrityStatus,
				"integrityStatus must not be null"
		);
		Objects.requireNonNull(
				classification,
				"classification must not be null"
		);
		Objects.requireNonNull(provenance, "provenance must not be null");
	}

	public static EvidenceGovernancePolicy govern(
			EvidenceQueryResult evidence,
			EvidenceProvenance provenance,
			boolean contradictoryEvidence
	) {
		Objects.requireNonNull(evidence, "evidence must not be null");
		Objects.requireNonNull(provenance, "provenance must not be null");

		return new EvidenceGovernancePolicy(
				trustLevel(provenance),
				integrityStatus(evidence, provenance, contradictoryEvidence),
				classification(evidence, provenance),
				provenance
		);
	}

	public boolean policyOnly() {
		return true;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean mutatesEvidence() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	public boolean operatorFacingAllowed() {
		return provenance.sanitized()
				&& classification != EvidenceClassification.BLOCKED
				&& classification != EvidenceClassification.GOVERNANCE_PROTECTED;
	}

	private static EvidenceTrustLevel trustLevel(
			EvidenceProvenance provenance
	) {
		if (provenance.provenanceMissing()) {
			return EvidenceTrustLevel.UNTRUSTED;
		}
		if (provenance.unknown()) {
			return EvidenceTrustLevel.UNKNOWN;
		}
		if (!provenance.sanitized()) {
			return EvidenceTrustLevel.PARTIALLY_TRUSTED;
		}
		return EvidenceTrustLevel.TRUSTED;
	}

	private static EvidenceIntegrityStatus integrityStatus(
			EvidenceQueryResult evidence,
			EvidenceProvenance provenance,
			boolean contradictoryEvidence
	) {
		if (provenance.provenanceMissing()) {
			return EvidenceIntegrityStatus.MISSING;
		}
		if (contradictoryEvidence) {
			return EvidenceIntegrityStatus.CONTRADICTORY;
		}
		if (provenance.rawPayloadPresent() || provenance.sensitiveDataPresent()) {
			return EvidenceIntegrityStatus.DEGRADED;
		}
		return switch (evidence.status()) {
			case ABSENT -> EvidenceIntegrityStatus.MISSING;
			case UNKNOWN, FAILED -> EvidenceIntegrityStatus.UNKNOWN;
			case PARTIAL -> EvidenceIntegrityStatus.DEGRADED;
			case COLLECTED -> EvidenceIntegrityStatus.INTACT;
		};
	}

	private static EvidenceClassification classification(
			EvidenceQueryResult evidence,
			EvidenceProvenance provenance
	) {
		if (provenance.sensitiveDataPresent()) {
			return EvidenceClassification.BLOCKED;
		}
		if (provenance.rawPayloadPresent()) {
			return EvidenceClassification.GOVERNANCE_PROTECTED;
		}
		if (evidence.sourceType() == EvidenceSourceType.PAYMENT_CONSISTENCY
				|| evidence.paymentConsistencyMetadataPresent()) {
			return EvidenceClassification.RESTRICTED;
		}
		if (!provenance.sanitized()) {
			return EvidenceClassification.INTERNAL;
		}
		if (provenance.provenanceMissing()) {
			return EvidenceClassification.UNKNOWN;
		}
		return EvidenceClassification.PUBLIC_SAFE;
	}
}
