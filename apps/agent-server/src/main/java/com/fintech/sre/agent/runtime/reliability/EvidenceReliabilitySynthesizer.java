package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class EvidenceReliabilitySynthesizer {

	public EvidenceReliability synthesize(
			EvidenceConfidence confidence
	) {
		Objects.requireNonNull(confidence, "confidence must not be null");

		EvidenceTrustScore trustScore = confidence.trustScore();
		EvidenceGovernancePolicy governancePolicy = trustScore
				.governanceIntegrationResult()
				.governancePolicy();
		EvidenceLineage lineage = trustScore.lineageIntegrationResult().lineage();

		return new EvidenceReliability(
				level(governancePolicy, lineage, trustScore, confidence),
				reason(governancePolicy, lineage, trustScore, confidence),
				scope(confidence),
				governancePolicy,
				lineage,
				trustScore,
				confidence,
				assessmentCertaintyAllowed(confidence),
				paymentSafetyUncertainty(governancePolicy, confidence)
		);
	}

	public boolean deterministicRuleBased() {
		return true;
	}

	public boolean mlInference() {
		return false;
	}

	public boolean bayesianInference() {
		return false;
	}

	public boolean weightingAlgorithm() {
		return false;
	}

	public boolean statisticalScoring() {
		return false;
	}

	private EvidenceReliabilityLevel level(
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage,
			EvidenceTrustScore trustScore,
			EvidenceConfidence confidence
	) {
		if (governancePolicy.classification() == EvidenceClassification.BLOCKED
				|| lineage.status() == EvidenceLineageStatus.BLOCKED) {
			return EvidenceReliabilityLevel.BLOCKED;
		}
		if (paymentSafetyUncertainty(governancePolicy, confidence)) {
			return EvidenceReliabilityLevel.RESTRICTED;
		}
		if (governancePolicy.classification() == EvidenceClassification.GOVERNANCE_PROTECTED
				|| governancePolicy.classification() == EvidenceClassification.RESTRICTED) {
			return EvidenceReliabilityLevel.RESTRICTED;
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.UNTRUSTED) {
			return EvidenceReliabilityLevel.UNRELIABLE;
		}
		if (contradictory(governancePolicy, lineage, trustScore, confidence)) {
			return EvidenceReliabilityLevel.LOW;
		}
		if (lineage.status() == EvidenceLineageStatus.INCOMPLETE) {
			return EvidenceReliabilityLevel.LOW;
		}
		if (confidence.level() == EvidenceConfidenceLevel.INSUFFICIENT) {
			return EvidenceReliabilityLevel.LOW;
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.LOW
				|| confidence.level() == EvidenceConfidenceLevel.LOW) {
			return EvidenceReliabilityLevel.LOW;
		}
		if (highReliability(governancePolicy, lineage, trustScore, confidence)) {
			return EvidenceReliabilityLevel.HIGH;
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.MEDIUM
				|| confidence.level() == EvidenceConfidenceLevel.MEDIUM) {
			return EvidenceReliabilityLevel.MEDIUM;
		}
		return EvidenceReliabilityLevel.UNKNOWN;
	}

	private EvidenceReliabilityReason reason(
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage,
			EvidenceTrustScore trustScore,
			EvidenceConfidence confidence
	) {
		if (governancePolicy.classification() == EvidenceClassification.BLOCKED
				|| lineage.status() == EvidenceLineageStatus.BLOCKED) {
			return EvidenceReliabilityReason.GOVERNANCE_BLOCKED;
		}
		if (paymentSafetyUncertainty(governancePolicy, confidence)) {
			return EvidenceReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (governancePolicy.classification() == EvidenceClassification.GOVERNANCE_PROTECTED
				|| governancePolicy.classification() == EvidenceClassification.RESTRICTED) {
			return EvidenceReliabilityReason.GOVERNANCE_RESTRICTED;
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.UNTRUSTED) {
			return EvidenceReliabilityReason.UNTRUSTED_EVIDENCE;
		}
		if (contradictory(governancePolicy, lineage, trustScore, confidence)) {
			return EvidenceReliabilityReason.CONTRADICTORY_EVIDENCE;
		}
		if (lineage.status() == EvidenceLineageStatus.INCOMPLETE) {
			return EvidenceReliabilityReason.INCOMPLETE_LINEAGE;
		}
		if (confidence.level() == EvidenceConfidenceLevel.INSUFFICIENT) {
			return EvidenceReliabilityReason.INSUFFICIENT_CONFIDENCE;
		}
		if (trustScore.level() == EvidenceTrustScoreLevel.LOW) {
			return EvidenceReliabilityReason.LOW_TRUST;
		}
		if (confidence.level() == EvidenceConfidenceLevel.LOW) {
			return EvidenceReliabilityReason.LOW_CONFIDENCE;
		}
		if (highReliability(governancePolicy, lineage, trustScore, confidence)) {
			return EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE;
		}
		return EvidenceReliabilityReason.UNKNOWN;
	}

	private EvidenceReliabilityScope scope(
			EvidenceConfidence confidence
	) {
		return switch (confidence.scope()) {
			case PAYMENT_EVIDENCE -> EvidenceReliabilityScope.PAYMENT_EVIDENCE;
			case ASSESSMENT -> EvidenceReliabilityScope.ASSESSMENT;
			case OBSERVABLE_RUNTIME -> EvidenceReliabilityScope.OBSERVABLE_RUNTIME;
			case OPERATOR_VIEW -> EvidenceReliabilityScope.OPERATOR_VIEW;
			case API_BOUNDARY -> EvidenceReliabilityScope.API_BOUNDARY;
			case EVIDENCE -> EvidenceReliabilityScope.EVIDENCE;
		};
	}

	private boolean assessmentCertaintyAllowed(
			EvidenceConfidence confidence
	) {
		return confidence.level() != EvidenceConfidenceLevel.INSUFFICIENT;
	}

	private boolean paymentSafetyUncertainty(
			EvidenceGovernancePolicy governancePolicy,
			EvidenceConfidence confidence
	) {
		return governancePolicy.classification() == EvidenceClassification.RESTRICTED
				&& (confidence.level() == EvidenceConfidenceLevel.INSUFFICIENT
				|| confidence.level() == EvidenceConfidenceLevel.LOW
				|| confidence.reason() == EvidenceConfidenceReason.PAYMENT_EVIDENCE_MISSING);
	}

	private boolean contradictory(
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage,
			EvidenceTrustScore trustScore,
			EvidenceConfidence confidence
	) {
		return governancePolicy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY
				|| lineage.reason() == EvidenceLineageReason.CONTRADICTORY_EVIDENCE
				|| trustScore.reason() == EvidenceTrustScoreReason.CONTRADICTORY_EVIDENCE
				|| confidence.reason() == EvidenceConfidenceReason.CONTRADICTORY_EVIDENCE;
	}

	private boolean highReliability(
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage,
			EvidenceTrustScore trustScore,
			EvidenceConfidence confidence
	) {
		return governancePolicy.classification() != EvidenceClassification.BLOCKED
				&& governancePolicy.classification()
				!= EvidenceClassification.GOVERNANCE_PROTECTED
				&& governancePolicy.classification() != EvidenceClassification.RESTRICTED
				&& lineage.status() == EvidenceLineageStatus.COMPLETE
				&& governancePolicy.provenance().sanitized()
				&& !governancePolicy.provenance().provenanceMissing()
				&& governancePolicy.trustLevel() == EvidenceTrustLevel.TRUSTED
				&& trustScore.level() == EvidenceTrustScoreLevel.HIGH
				&& confidence.level() == EvidenceConfidenceLevel.HIGH;
	}
}
