package com.fintech.sre.agent.runtime.recommendation;

import java.util.Objects;

import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadiness;
import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadinessLevel;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingDecision;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingRejectionReason;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingDecision;

public class RecommendationCandidateEvaluator {

	public RecommendationCandidate evaluate(
			ActionAdmissionReadiness actionAdmissionReadiness,
			String runbookBinding
	) {
		Objects.requireNonNull(
				actionAdmissionReadiness,
				"actionAdmissionReadiness must not be null"
		);

		return new RecommendationCandidate(
				level(actionAdmissionReadiness, runbookBinding),
				reason(actionAdmissionReadiness, runbookBinding),
				scope(actionAdmissionReadiness, runbookBinding),
				actionAdmissionReadiness,
				runbookBinding
		);
	}

	private RecommendationCandidateLevel level(
			ActionAdmissionReadiness actionAdmissionReadiness,
			String runbookBinding
	) {
		if (paymentSafetyUncertainty(actionAdmissionReadiness)) {
			return RecommendationCandidateLevel.BLOCKED;
		}
		if (criticalLifecycleRisk(actionAdmissionReadiness)) {
			return RecommendationCandidateLevel.BLOCKED;
		}
		if (missingScenarioBinding(actionAdmissionReadiness)) {
			return RecommendationCandidateLevel.BLOCKED;
		}
		if (missingRunbookBinding(runbookBinding)) {
			return RecommendationCandidateLevel.BLOCKED;
		}
		if (missingRollbackBinding(actionAdmissionReadiness)) {
			return RecommendationCandidateLevel.BLOCKED;
		}
		if (missingVerificationBinding(actionAdmissionReadiness)) {
			return RecommendationCandidateLevel.BLOCKED;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.BLOCKED) {
			return RecommendationCandidateLevel.BLOCKED;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.UNRELIABLE) {
			return RecommendationCandidateLevel.UNRELIABLE;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.NOT_READY) {
			return RecommendationCandidateLevel.NOT_READY;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.PARTIAL) {
			return RecommendationCandidateLevel.PARTIAL;
		}
		if (eligible(actionAdmissionReadiness, runbookBinding)) {
			return RecommendationCandidateLevel.ELIGIBLE;
		}
		return RecommendationCandidateLevel.UNKNOWN;
	}

	private RecommendationCandidateReason reason(
			ActionAdmissionReadiness actionAdmissionReadiness,
			String runbookBinding
	) {
		if (paymentSafetyUncertainty(actionAdmissionReadiness)) {
			return RecommendationCandidateReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (criticalLifecycleRisk(actionAdmissionReadiness)) {
			return RecommendationCandidateReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingScenarioBinding(actionAdmissionReadiness)) {
			return RecommendationCandidateReason.MISSING_SCENARIO_BINDING;
		}
		if (missingRunbookBinding(runbookBinding)) {
			return RecommendationCandidateReason.MISSING_RUNBOOK_BINDING;
		}
		if (missingRollbackBinding(actionAdmissionReadiness)) {
			return RecommendationCandidateReason.MISSING_ROLLBACK_BINDING;
		}
		if (missingVerificationBinding(actionAdmissionReadiness)) {
			return RecommendationCandidateReason.MISSING_VERIFICATION_BINDING;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.BLOCKED) {
			return RecommendationCandidateReason.BLOCKED_ACTION_ADMISSION;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.UNRELIABLE) {
			return RecommendationCandidateReason.UNRELIABLE_ACTION_ADMISSION;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.NOT_READY) {
			return RecommendationCandidateReason.NOT_READY_ACTION_ADMISSION;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.PARTIAL) {
			return RecommendationCandidateReason.PARTIAL_ACTION_ADMISSION;
		}
		if (eligible(actionAdmissionReadiness, runbookBinding)) {
			return RecommendationCandidateReason.ELIGIBLE_ACTION_ADMISSION;
		}
		return RecommendationCandidateReason.UNKNOWN;
	}

	private RecommendationCandidateScope scope(
			ActionAdmissionReadiness actionAdmissionReadiness,
			String runbookBinding
	) {
		if (paymentSafetyUncertainty(actionAdmissionReadiness)) {
			return RecommendationCandidateScope.PAYMENT_SAFETY;
		}
		if (criticalLifecycleRisk(actionAdmissionReadiness)) {
			return RecommendationCandidateScope.LIFECYCLE_RISK;
		}
		if (missingScenarioBinding(actionAdmissionReadiness)) {
			return RecommendationCandidateScope.SCENARIO;
		}
		if (missingRunbookBinding(runbookBinding)) {
			return RecommendationCandidateScope.RUNBOOK;
		}
		if (missingRollbackBinding(actionAdmissionReadiness)) {
			return RecommendationCandidateScope.ROLLBACK;
		}
		if (missingVerificationBinding(actionAdmissionReadiness)) {
			return RecommendationCandidateScope.VERIFICATION;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.BLOCKED
				|| actionAdmissionReadiness.level()
				== ActionAdmissionReadinessLevel.UNRELIABLE) {
			return RecommendationCandidateScope.ACTION_ADMISSION_READINESS;
		}
		if (actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.NOT_READY
				|| actionAdmissionReadiness.level()
				== ActionAdmissionReadinessLevel.PARTIAL) {
			return RecommendationCandidateScope.ACTION_ADMISSION_READINESS;
		}
		return RecommendationCandidateScope.RECOMMENDATION_CANDIDATE;
	}

	private boolean eligible(
			ActionAdmissionReadiness actionAdmissionReadiness,
			String runbookBinding
	) {
		return actionAdmissionReadiness.level() == ActionAdmissionReadinessLevel.READY
				&& !missingScenarioBinding(actionAdmissionReadiness)
				&& !missingRunbookBinding(runbookBinding)
				&& !missingRollbackBinding(actionAdmissionReadiness)
				&& !missingVerificationBinding(actionAdmissionReadiness)
				&& !paymentSafetyUncertainty(actionAdmissionReadiness)
				&& !criticalLifecycleRisk(actionAdmissionReadiness);
	}

	private boolean missingScenarioBinding(
			ActionAdmissionReadiness actionAdmissionReadiness
	) {
		ScenarioBindingDecision decision = actionAdmissionReadiness
				.verificationReadiness()
				.approvalReadiness()
				.recommendationReadiness()
				.recommendationReliability()
				.decisionReliability()
				.scenarioBindingDecision();
		return decision == null || !decision.recommendationScenarioAvailable();
	}

	private boolean missingRunbookBinding(String runbookBinding) {
		return runbookBinding == null || runbookBinding.isBlank();
	}

	private boolean missingRollbackBinding(
			ActionAdmissionReadiness actionAdmissionReadiness
	) {
		RollbackVerificationBindingDecision decision = actionAdmissionReadiness
				.verificationReadiness()
				.approvalReadiness()
				.recommendationReadiness()
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE;
	}

	private boolean missingVerificationBinding(
			ActionAdmissionReadiness actionAdmissionReadiness
	) {
		RollbackVerificationBindingDecision decision = actionAdmissionReadiness
				.verificationReadiness()
				.approvalReadiness()
				.recommendationReadiness()
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE;
	}

	private boolean paymentSafetyUncertainty(
			ActionAdmissionReadiness actionAdmissionReadiness
	) {
		return actionAdmissionReadiness.verificationReadiness()
				.approvalReadiness()
				.recommendationReadiness()
				.reason()
				== com.fintech.sre.agent.runtime.readiness.RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY;
	}

	private boolean criticalLifecycleRisk(
			ActionAdmissionReadiness actionAdmissionReadiness
	) {
		return actionAdmissionReadiness.verificationReadiness()
				.approvalReadiness()
				.recommendationReadiness()
				.lifecycleRisk() == OperationalUncertainty.CRITICAL;
	}
}
