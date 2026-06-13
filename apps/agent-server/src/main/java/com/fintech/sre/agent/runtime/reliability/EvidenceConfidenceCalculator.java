package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceConfidenceCalculator {

	public EvidenceConfidence calculate(
			EvidenceTrustScore trustScore
	) {
		Objects.requireNonNull(trustScore, "trustScore must not be null");

		return new EvidenceConfidence(
				level(trustScore),
				reason(trustScore),
				scope(trustScore),
				trustScore
		);
	}

	public boolean deterministicRuleBased() {
		return true;
	}

	public boolean numericScore() {
		return false;
	}

	public boolean weightingAlgorithm() {
		return false;
	}

	public boolean mlInference() {
		return false;
	}

	public boolean bayesianInference() {
		return false;
	}

	public boolean statisticalConfidence() {
		return false;
	}

	private EvidenceConfidenceLevel level(
			EvidenceTrustScore trustScore
	) {
		EvidenceGovernancePolicy policy = trustScore.governanceIntegrationResult()
				.governancePolicy();
		EvidenceLineage lineage = trustScore.lineageIntegrationResult().lineage();

		if (policy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY) {
			return EvidenceConfidenceLevel.LOW;
		}
		if (policy.classification() == EvidenceClassification.RESTRICTED
				&& policy.provenance().provenanceMissing()) {
			return EvidenceConfidenceLevel.INSUFFICIENT;
		}
		if (policy.classification() == EvidenceClassification.RESTRICTED
				&& lineage.status() != EvidenceLineageStatus.COMPLETE) {
			return EvidenceConfidenceLevel.INSUFFICIENT;
		}
		if (policy.classification() == EvidenceClassification.RESTRICTED
				&& policy.integrityStatus() != EvidenceIntegrityStatus.INTACT) {
			return EvidenceConfidenceLevel.LOW;
		}
		if (policy.provenance().provenanceMissing()) {
			return EvidenceConfidenceLevel.INSUFFICIENT;
		}
		if (lineage.status() == EvidenceLineageStatus.INCOMPLETE) {
			return EvidenceConfidenceLevel.INSUFFICIENT;
		}
		if (policy.integrityStatus() == EvidenceIntegrityStatus.UNKNOWN
				|| lineage.status() == EvidenceLineageStatus.UNKNOWN
				|| trustScore.level() == EvidenceTrustScoreLevel.UNKNOWN) {
			return EvidenceConfidenceLevel.LOW;
		}
		if (policy.integrityStatus() == EvidenceIntegrityStatus.DEGRADED
				|| trustScore.level() == EvidenceTrustScoreLevel.LOW) {
			return EvidenceConfidenceLevel.LOW;
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.MEDIUM) {
			return EvidenceConfidenceLevel.MEDIUM;
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.HIGH
				&& lineage.nodes().contains(EvidenceLineageNode.COLLECTION)
				&& lineage.nodes().contains(EvidenceLineageNode.ASSESSMENT)
				&& policy.integrityStatus() == EvidenceIntegrityStatus.INTACT) {
			return EvidenceConfidenceLevel.HIGH;
		}
		return EvidenceConfidenceLevel.UNKNOWN;
	}

	private EvidenceConfidenceReason reason(
			EvidenceTrustScore trustScore
	) {
		EvidenceGovernancePolicy policy = trustScore.governanceIntegrationResult()
				.governancePolicy();
		EvidenceLineage lineage = trustScore.lineageIntegrationResult().lineage();

		if (policy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY) {
			return EvidenceConfidenceReason.CONTRADICTORY_EVIDENCE;
		}
		if (policy.classification() == EvidenceClassification.RESTRICTED
				&& (policy.provenance().provenanceMissing()
				|| lineage.status() != EvidenceLineageStatus.COMPLETE)) {
			return EvidenceConfidenceReason.PAYMENT_EVIDENCE_MISSING;
		}
		if (policy.provenance().provenanceMissing()
				|| lineage.status() == EvidenceLineageStatus.INCOMPLETE) {
			return EvidenceConfidenceReason.INSUFFICIENT_EVIDENCE;
		}
		if (policy.integrityStatus() == EvidenceIntegrityStatus.UNKNOWN
				|| lineage.status() == EvidenceLineageStatus.UNKNOWN
				|| trustScore.level() == EvidenceTrustScoreLevel.UNKNOWN) {
			return EvidenceConfidenceReason.UNKNOWN_EVIDENCE;
		}
		if (policy.integrityStatus() == EvidenceIntegrityStatus.DEGRADED
				|| trustScore.level() == EvidenceTrustScoreLevel.LOW) {
			return EvidenceConfidenceReason.PARTIAL_EVIDENCE;
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.MEDIUM) {
			return EvidenceConfidenceReason.PARTIAL_EVIDENCE;
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.HIGH
				&& lineage.nodes().contains(EvidenceLineageNode.COLLECTION)
				&& lineage.nodes().contains(EvidenceLineageNode.ASSESSMENT)
				&& policy.integrityStatus() == EvidenceIntegrityStatus.INTACT) {
			return EvidenceConfidenceReason.CORROBORATING_EVIDENCE;
		}
		return EvidenceConfidenceReason.UNKNOWN;
	}

	private EvidenceConfidenceScope scope(
			EvidenceTrustScore trustScore
	) {
		return trustScore.scope() == EvidenceTrustScoreScope.PAYMENT_EVIDENCE
				? EvidenceConfidenceScope.PAYMENT_EVIDENCE
				: EvidenceConfidenceScope.EVIDENCE;
	}
}
