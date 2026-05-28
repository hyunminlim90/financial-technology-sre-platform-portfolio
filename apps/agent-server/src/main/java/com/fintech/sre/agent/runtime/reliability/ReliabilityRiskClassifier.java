package com.fintech.sre.agent.runtime.reliability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReliabilityRiskClassifier {

	public ReliabilityRiskClassification classify(
			ReliabilityAssessmentResult assessmentResult
	) {
		Objects.requireNonNull(
				assessmentResult,
				"assessmentResult must not be null"
		);

		List<ReliabilityRiskFactor> factors = new ArrayList<>();
		ReliabilityRiskLevel level = ReliabilityRiskLevel.MEDIUM;
		ReliabilityRiskReason reason =
				ReliabilityRiskReason.DEFAULT_MODERATE_RUNTIME_RISK;

		if (assessmentResult.runtimeState() == RuntimeState.FAILED) {
			factors.add(ReliabilityRiskFactor.FAILED_STATE);
			return new ReliabilityRiskClassification(
					ReliabilityRiskLevel.CRITICAL,
					factors,
					ReliabilityRiskReason.FAILED_STATE_IS_CRITICAL
			);
		}

		EvidenceCorrelation correlation = assessmentResult.evidenceCorrelation();
		boolean partialEvidence =
				correlation.completeness() == EvidenceCompleteness.PARTIAL;
		boolean absentEvidence =
				correlation.completeness() == EvidenceCompleteness.ABSENT;
		boolean regressionDetected = assessmentResult.regressionDecision() != null
				&& assessmentResult.regressionDecision().regressionDetected();
		boolean propagationActive = assessmentResult.convergenceDecision() != null
				&& assessmentResult.convergenceDecision()
						.assessment()
						.propagationActive();

		if (correlation.paymentSafetyUncertain()) {
			factors.add(ReliabilityRiskFactor.PAYMENT_SAFETY_UNCERTAINTY);
			level = max(level, ReliabilityRiskLevel.HIGH);
			reason =
					ReliabilityRiskReason
							.PAYMENT_SAFETY_UNCERTAINTY_REQUIRES_HIGH_RISK;
		}

		if (correlation.contradictoryEvidence()) {
			factors.add(ReliabilityRiskFactor.CONTRADICTORY_EVIDENCE);
			level = max(level, ReliabilityRiskLevel.HIGH);
			reason =
					ReliabilityRiskReason.CONTRADICTORY_EVIDENCE_REQUIRES_HIGH_RISK;
		}

		if (propagationActive && partialEvidence) {
			factors.add(
					ReliabilityRiskFactor.ACTIVE_PROPAGATION_WITH_PARTIAL_EVIDENCE
			);
			level = max(level, ReliabilityRiskLevel.HIGH);
			reason =
					ReliabilityRiskReason
							.ACTIVE_PROPAGATION_WITH_PARTIAL_EVIDENCE_REQUIRES_HIGH_RISK;
		}

		if (regressionDetected) {
			factors.add(ReliabilityRiskFactor.POST_CONVERGENCE_REGRESSION);
			level = max(level, ReliabilityRiskLevel.HIGH);
			reason =
					ReliabilityRiskReason.POST_CONVERGENCE_REGRESSION_REQUIRES_ELEVATED_RISK;
		}

		if (assessmentResult.runtimeState() == RuntimeState.UNKNOWN) {
			factors.add(ReliabilityRiskFactor.UNKNOWN_RUNTIME_STATE);
			level = max(level, ReliabilityRiskLevel.MEDIUM);
			if (reason == ReliabilityRiskReason.DEFAULT_MODERATE_RUNTIME_RISK) {
				reason = ReliabilityRiskReason.UNKNOWN_RUNTIME_STATE_PREVENTS_LOW_RISK;
			}
		}

		if (absentEvidence) {
			factors.add(ReliabilityRiskFactor.ABSENT_EVIDENCE);
			level = max(level, ReliabilityRiskLevel.MEDIUM);
			if (reason == ReliabilityRiskReason.DEFAULT_MODERATE_RUNTIME_RISK) {
				reason = ReliabilityRiskReason.ABSENT_EVIDENCE_PREVENTS_LOW_RISK;
			}
		}

		if (partialEvidence) {
			factors.add(ReliabilityRiskFactor.PARTIAL_EVIDENCE);
			level = max(level, ReliabilityRiskLevel.MEDIUM);
			if (reason == ReliabilityRiskReason.DEFAULT_MODERATE_RUNTIME_RISK) {
				reason = ReliabilityRiskReason.PARTIAL_EVIDENCE_PREVENTS_LOW_RISK;
			}
		}

		if (isLowCandidate(assessmentResult)) {
			factors.add(ReliabilityRiskFactor.STABLE_CONVERGED_COMPLETE_EVIDENCE);
			return new ReliabilityRiskClassification(
					ReliabilityRiskLevel.LOW,
					factors,
					ReliabilityRiskReason
							.STABLE_CONVERGED_COMPLETE_EVIDENCE_SUPPORTS_LOW_RISK
			);
		}

		return new ReliabilityRiskClassification(level, factors, reason);
	}

	private boolean isLowCandidate(
			ReliabilityAssessmentResult assessmentResult
	) {
		EvidenceCorrelation correlation = assessmentResult.evidenceCorrelation();
		boolean completeEvidence =
				correlation.completeness() == EvidenceCompleteness.COMPLETE;
		boolean noRegression = assessmentResult.regressionDecision() == null
				|| !assessmentResult.regressionDecision().regressionDetected();

		return assessmentResult.runtimeState() == RuntimeState.CONVERGED
				&& noRegression
				&& completeEvidence
				&& !correlation.paymentSafetyUncertain()
				&& !correlation.contradictoryEvidence();
	}

	private ReliabilityRiskLevel max(
			ReliabilityRiskLevel left,
			ReliabilityRiskLevel right
	) {
		return left.ordinal() >= right.ordinal() ? left : right;
	}
}
