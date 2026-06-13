package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class AssessmentReliabilityEvaluator {

	public AssessmentReliability evaluate(
			EvidenceReliability evidenceReliability
	) {
		Objects.requireNonNull(
				evidenceReliability,
				"evidenceReliability must not be null"
		);

		return new AssessmentReliability(
				level(evidenceReliability),
				reason(evidenceReliability),
				scope(evidenceReliability),
				evidenceReliability,
				assessmentCertaintyAllowed(evidenceReliability)
		);
	}

	private AssessmentReliabilityLevel level(
			EvidenceReliability evidenceReliability
	) {
		if (evidenceReliability.level() == EvidenceReliabilityLevel.BLOCKED) {
			return AssessmentReliabilityLevel.BLOCKED;
		}
		if (evidenceReliability.level() == EvidenceReliabilityLevel.UNRELIABLE) {
			return AssessmentReliabilityLevel.UNRELIABLE;
		}
		if (contradictory(evidenceReliability)) {
			return evidenceReliability.level() == EvidenceReliabilityLevel.UNRELIABLE
					? AssessmentReliabilityLevel.UNRELIABLE
					: AssessmentReliabilityLevel.LOW;
		}
		if (evidenceReliability.paymentSafetyUncertainty()) {
			return AssessmentReliabilityLevel.LOW;
		}
		if (!evidenceReliability.assessmentCertaintyAllowed()) {
			return AssessmentReliabilityLevel.LOW;
		}
		if (evidenceReliability.level() == EvidenceReliabilityLevel.LOW) {
			return AssessmentReliabilityLevel.LOW;
		}
		if (evidenceReliability.level() == EvidenceReliabilityLevel.HIGH
				&& !evidenceReliability.paymentSafetyUncertainty()
				&& !contradictory(evidenceReliability)) {
			return AssessmentReliabilityLevel.HIGH;
		}
		if (evidenceReliability.level() == EvidenceReliabilityLevel.MEDIUM
				|| evidenceReliability.level() == EvidenceReliabilityLevel.RESTRICTED) {
			return AssessmentReliabilityLevel.MEDIUM;
		}
		return AssessmentReliabilityLevel.UNKNOWN;
	}

	private AssessmentReliabilityReason reason(
			EvidenceReliability evidenceReliability
	) {
		if (evidenceReliability.level() == EvidenceReliabilityLevel.BLOCKED) {
			return AssessmentReliabilityReason.BLOCKED_EVIDENCE;
		}
		if (evidenceReliability.level() == EvidenceReliabilityLevel.UNRELIABLE) {
			return AssessmentReliabilityReason.UNRELIABLE_EVIDENCE;
		}
		if (contradictory(evidenceReliability)) {
			return AssessmentReliabilityReason.CONTRADICTORY_EVIDENCE;
		}
		if (evidenceReliability.paymentSafetyUncertainty()) {
			return AssessmentReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (!evidenceReliability.assessmentCertaintyAllowed()) {
			return AssessmentReliabilityReason.INSUFFICIENT_CONFIDENCE;
		}
		if (evidenceReliability.level() == EvidenceReliabilityLevel.LOW) {
			return AssessmentReliabilityReason.LOW_EVIDENCE_RELIABILITY;
		}
		if (evidenceReliability.level() == EvidenceReliabilityLevel.HIGH
				&& !evidenceReliability.paymentSafetyUncertainty()
				&& !contradictory(evidenceReliability)) {
			return AssessmentReliabilityReason.HIGH_EVIDENCE_RELIABILITY;
		}
		return AssessmentReliabilityReason.UNKNOWN;
	}

	private AssessmentReliabilityScope scope(
			EvidenceReliability evidenceReliability
	) {
		return switch (evidenceReliability.scope()) {
			case PAYMENT_EVIDENCE -> AssessmentReliabilityScope.PAYMENT_EVIDENCE;
			case OBSERVABLE_RUNTIME -> AssessmentReliabilityScope.OBSERVABLE_RUNTIME;
			case OPERATOR_VIEW -> AssessmentReliabilityScope.OPERATOR_VIEW;
			case API_BOUNDARY -> AssessmentReliabilityScope.API_BOUNDARY;
			case ASSESSMENT, EVIDENCE -> AssessmentReliabilityScope.ASSESSMENT;
		};
	}

	private boolean assessmentCertaintyAllowed(
			EvidenceReliability evidenceReliability
	) {
		return evidenceReliability.assessmentCertaintyAllowed()
				&& evidenceReliability.level() != EvidenceReliabilityLevel.BLOCKED
				&& evidenceReliability.level() != EvidenceReliabilityLevel.UNRELIABLE;
	}

	private boolean contradictory(
			EvidenceReliability evidenceReliability
	) {
		return evidenceReliability.reason()
				== EvidenceReliabilityReason.CONTRADICTORY_EVIDENCE;
	}
}
