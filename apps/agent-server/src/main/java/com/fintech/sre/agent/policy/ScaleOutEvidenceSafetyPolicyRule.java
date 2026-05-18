package com.fintech.sre.agent.policy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceSignal;

import reactor.core.publisher.Mono;

@Component
public class ScaleOutEvidenceSafetyPolicyRule implements PolicyRule {

	@Override
	public Mono<PolicyEvaluationResult> evaluate(
			ActionCommand command,
			EvidenceContext evidence
	) {
		if (command == null || command.type() != ActionType.SCALE_OUT) {
			return Mono.just(PolicyEvaluationResult.allow());
		}

		if (evidence == null || !evidence.hasOperationalSignals()) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_SCALE_OUT_EVIDENCE_REQUIRED",
							PolicySeverity.BLOCKING,
							"Scale out 추천에는 operational evidence가 필요합니다.",
							null
					)
			)));
		}

		if (evidence.hasDatabaseSaturation()) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_SCALE_OUT_BLOCKED_DB_SATURATION",
							PolicySeverity.BLOCKING,
							"Database saturation이 감지되어 Scale Out을 차단합니다.",
							EvidenceSignal.DB_POOL_PENDING_HIGH.code()
					)
			)));
		}

		if (evidence.hasKafkaRebalanceStorm()) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_SCALE_OUT_BLOCKED_REBALANCE_STORM",
							PolicySeverity.BLOCKING,
							"Kafka rebalance storm가 감지되어 Scale Out을 차단합니다.",
							EvidenceSignal.KAFKA_REBALANCE_STORM.code()
					)
			)));
		}

		if (evidence.hasRetryStorm()) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_SCALE_OUT_BLOCKED_RETRY_STORM",
							PolicySeverity.BLOCKING,
							"Retry storm가 감지되어 Scale Out을 차단합니다.",
							EvidenceSignal.RETRY_STORM.code()
					)
			)));
		}

		if (!evidence.hasReliableSignal(EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH)) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_SCALE_OUT_REQUIRES_CONSUMER_LAG",
							PolicySeverity.BLOCKING,
							"Scale Out에는 Kafka consumer lag evidence가 필요합니다.",
							EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH.code()
					)
			)));
		}

		return Mono.just(PolicyEvaluationResult.allow());
	}
}
