package com.fintech.sre.agent.decision.policy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.decision.CandidateAction;
import com.fintech.sre.agent.decision.DecisionInput;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceNormalizer;
import com.fintech.sre.agent.evidence.EvidenceSignal;

@Component
public class ActionPolicyEngine {

	private final EvidenceNormalizer evidenceNormalizer;

	public ActionPolicyEngine() {
		this(new EvidenceNormalizer());
	}

	public ActionPolicyEngine(EvidenceNormalizer evidenceNormalizer) {
		this.evidenceNormalizer = evidenceNormalizer;
	}

	public PolicyEvaluationResult evaluate(DecisionInput input, CandidateAction action) {
		EvidenceContext evidenceContext = evidenceNormalizer.normalize(extractEvidenceFlags(input));
		PolicyEvaluationResult result = evaluate(action.command(), evidenceContext);
		if (!result.allowed()) {
			return result;
		}
		if (action.command() != null
				&& action.command().rollback() != null
				&& (action.rollbackPlan() == null || action.rollbackPlan().isBlank())) {
			return PolicyEvaluationResult.deny("rollback-required", "Rollback-required action needs rollback plan");
		}
		if (action.command() != null
				&& action.command().verifications() != null
				&& !action.command().verifications().isEmpty()
				&& (action.verification() == null || action.verification().isEmpty())) {
			return PolicyEvaluationResult.deny("verification-required", "Verification-required action needs checks");
		}
		return result;
	}

	@Deprecated
	public PolicyEvaluationResult evaluate(ActionCommand command, List<String> evidenceFlags) {
		return evaluate(command, evidenceNormalizer.normalize(evidenceFlags));
	}

	public PolicyEvaluationResult evaluate(ActionCommand command, EvidenceContext evidence) {
		if (command == null) {
			return PolicyEvaluationResult.deny(
					"action-command-required",
					"ActionCommand is required for policy evaluation"
			);
		}

		if (evidence == null || evidence.evidences() == null || evidence.evidences().isEmpty()) {
			return PolicyEvaluationResult.insufficientEvidence(
					"evidence-required",
					"Evidence is required before recommending operational actions"
			);
		}

		if (evidence.observabilityDegraded()) {
			return PolicyEvaluationResult.requireApproval(
					"observability-degraded",
					"Observability source is degraded. Human approval is required."
			);
		}

		if (command.type() == ActionType.SCALE_OUT) {
			return evaluateScaleOutWorker(command, evidence);
		}

		if (!command.requiresHumanApproval()) {
			return PolicyEvaluationResult.deny(
					"human-approval-required",
					"Operational actions must require human approval"
			);
		}

		if (command.rollback() == null) {
			return PolicyEvaluationResult.deny(
					"rollback-required",
					"Operational action must define rollback"
			);
		}

		if (command.verifications() == null || command.verifications().isEmpty()) {
			return PolicyEvaluationResult.deny(
					"verification-required",
					"Operational action must define verification"
			);
		}

		return PolicyEvaluationResult.allow();
	}

	private PolicyEvaluationResult evaluateScaleOutWorker(ActionCommand command, EvidenceContext evidence) {
		if (evidence.hasDatabaseSaturation()) {
			return PolicyEvaluationResult.deny(
					"scaleout-blocked-db-saturation",
					"Worker scale-out is blocked because database saturation is detected"
			);
		}

		if (evidence.hasKafkaRebalanceStorm()) {
			return PolicyEvaluationResult.deny(
					"scaleout-blocked-rebalance-storm",
					"Worker scale-out is blocked because Kafka rebalance storm is detected"
			);
		}

		if (evidence.hasRetryStorm()) {
			return PolicyEvaluationResult.deny(
					"scaleout-blocked-retry-storm",
					"Worker scale-out is blocked because retry storm is detected"
			);
		}

		if (!evidence.hasReliableSignal(EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH)) {
			return PolicyEvaluationResult.insufficientEvidence(
					"scaleout-requires-consumer-lag-evidence",
					"Worker scale-out requires reliable Kafka consumer lag evidence"
			);
		}

		if (!command.requiresHumanApproval()) {
			return PolicyEvaluationResult.deny(
					"scaleout-approval-required",
					"Worker scale-out requires human approval"
			);
		}

		return PolicyEvaluationResult.allow();
	}

	private List<String> extractEvidenceFlags(DecisionInput input) {
		java.util.ArrayList<String> flags = new java.util.ArrayList<>();
		var metrics = input.incidentContext().metricsSnapshot();
		if (metrics != null) {
			if (metrics.dbConnectionPending() != null && metrics.dbConnectionPending() > 0) {
				flags.add("DB_POOL_SATURATED");
			}
			if (metrics.retryRate() != null && metrics.retryRate() >= 0.20) {
				flags.add("RETRY_STORM");
				flags.add("RETRY_RATE_HIGH");
			}
			if (metrics.kafkaConsumerLag() != null) {
				if (metrics.kafkaConsumerLag() > 1_000) {
					flags.add("KAFKA_CONSUMER_LAG_HIGH");
				}
				if (metrics.kafkaConsumerLag() > 5_000) {
					flags.add("REBALANCE_STORM");
				}
			}
		}
		if (input.incidentContext().evidence().logs().stream()
				.anyMatch(log -> log.toLowerCase().contains("rebalance storm"))) {
			flags.add("REBALANCE_STORM");
		}
		return flags.stream().distinct().toList();
	}
}
