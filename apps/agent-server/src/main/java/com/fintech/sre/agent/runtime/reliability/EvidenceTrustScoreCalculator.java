package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceTrustScoreCalculator {

	public EvidenceTrustScore calculate(
			EvidenceGovernanceIntegrationResult governanceIntegrationResult,
			EvidenceLineageIntegrationResult lineageIntegrationResult
	) {
		Objects.requireNonNull(
				governanceIntegrationResult,
				"governanceIntegrationResult must not be null"
		);
		Objects.requireNonNull(
				lineageIntegrationResult,
				"lineageIntegrationResult must not be null"
		);

		return new EvidenceTrustScore(
				level(governanceIntegrationResult, lineageIntegrationResult),
				reason(governanceIntegrationResult, lineageIntegrationResult),
				scope(governanceIntegrationResult),
				governanceIntegrationResult,
				lineageIntegrationResult
		);
	}

	public boolean deterministicRuleBased() {
		return true;
	}

	public boolean numericScore() {
		return false;
	}

	public boolean weightedAlgorithm() {
		return false;
	}

	public boolean mlInference() {
		return false;
	}

	private EvidenceTrustScoreLevel level(
			EvidenceGovernanceIntegrationResult governanceIntegrationResult,
			EvidenceLineageIntegrationResult lineageIntegrationResult
	) {
		EvidenceGovernancePolicy policy = governanceIntegrationResult.governancePolicy();

		if (policy.classification() == EvidenceClassification.BLOCKED) {
			return EvidenceTrustScoreLevel.UNTRUSTED;
		}
		if (policy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY) {
			return EvidenceTrustScoreLevel.LOW;
		}
		if (policy.provenance().provenanceMissing()) {
			return EvidenceTrustScoreLevel.LOW;
		}
		if (lineageIntegrationResult.lineage().status() == EvidenceLineageStatus.INCOMPLETE) {
			return EvidenceTrustScoreLevel.LOW;
		}
		if (policy.trustLevel() == EvidenceTrustLevel.PARTIALLY_TRUSTED
				|| policy.trustLevel() == EvidenceTrustLevel.UNKNOWN) {
			return EvidenceTrustScoreLevel.MEDIUM;
		}
		if (policy.integrityStatus() == EvidenceIntegrityStatus.DEGRADED
				|| policy.classification() == EvidenceClassification.RESTRICTED
				|| policy.classification() == EvidenceClassification.GOVERNANCE_PROTECTED) {
			return EvidenceTrustScoreLevel.MEDIUM;
		}
		if (policy.trustLevel() == EvidenceTrustLevel.TRUSTED
				&& lineageIntegrationResult.lineage().status() == EvidenceLineageStatus.COMPLETE
				&& policy.integrityStatus() == EvidenceIntegrityStatus.INTACT) {
			return EvidenceTrustScoreLevel.HIGH;
		}
		return EvidenceTrustScoreLevel.UNKNOWN;
	}

	private EvidenceTrustScoreReason reason(
			EvidenceGovernanceIntegrationResult governanceIntegrationResult,
			EvidenceLineageIntegrationResult lineageIntegrationResult
	) {
		EvidenceGovernancePolicy policy = governanceIntegrationResult.governancePolicy();

		if (policy.classification() == EvidenceClassification.BLOCKED) {
			return EvidenceTrustScoreReason.BLOCKED_EVIDENCE;
		}
		if (policy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY) {
			return EvidenceTrustScoreReason.CONTRADICTORY_EVIDENCE;
		}
		if (policy.provenance().provenanceMissing()) {
			return EvidenceTrustScoreReason.MISSING_PROVENANCE;
		}
		if (lineageIntegrationResult.lineage().status() == EvidenceLineageStatus.INCOMPLETE) {
			return EvidenceTrustScoreReason.INCOMPLETE_LINEAGE;
		}
		if (policy.classification() == EvidenceClassification.RESTRICTED) {
			return EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE;
		}
		if (policy.integrityStatus() == EvidenceIntegrityStatus.DEGRADED) {
			return EvidenceTrustScoreReason.DEGRADED_INTEGRITY;
		}
		if (policy.trustLevel() == EvidenceTrustLevel.PARTIALLY_TRUSTED
				|| policy.trustLevel() == EvidenceTrustLevel.UNKNOWN) {
			return EvidenceTrustScoreReason.PARTIAL_PROVENANCE;
		}
		if (policy.trustLevel() == EvidenceTrustLevel.TRUSTED
				&& lineageIntegrationResult.lineage().status() == EvidenceLineageStatus.COMPLETE
				&& policy.integrityStatus() == EvidenceIntegrityStatus.INTACT) {
			return EvidenceTrustScoreReason.TRUSTED_PROVENANCE;
		}
		return EvidenceTrustScoreReason.UNKNOWN;
	}

	private EvidenceTrustScoreScope scope(
			EvidenceGovernanceIntegrationResult governanceIntegrationResult
	) {
		return governanceIntegrationResult.governancePolicy().classification()
				== EvidenceClassification.RESTRICTED
				? EvidenceTrustScoreScope.PAYMENT_EVIDENCE
				: EvidenceTrustScoreScope.EVIDENCE;
	}
}
