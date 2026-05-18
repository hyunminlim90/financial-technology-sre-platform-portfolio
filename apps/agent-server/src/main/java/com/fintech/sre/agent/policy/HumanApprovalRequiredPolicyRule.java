package com.fintech.sre.agent.policy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.evidence.EvidenceContext;

import reactor.core.publisher.Mono;

@Component
public class HumanApprovalRequiredPolicyRule implements PolicyRule {

	@Override
	public Mono<PolicyEvaluationResult> evaluate(ActionCommand command, EvidenceContext evidence) {
		if (command == null) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_MISSING_ACTION_COMMAND",
							PolicySeverity.BLOCKING,
							"ActionCommand가 없으므로 추천할 수 없습니다."
					)
			)));
		}

		if (!command.requiresHumanApproval()) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_HUMAN_APPROVAL_REQUIRED",
							PolicySeverity.BLOCKING,
							"Human approval 없는 ActionCommand는 허용되지 않습니다."
					)
			)));
		}

		if (command.isHighRiskOrAbove() && !command.requiresHumanApproval()) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_HIGH_RISK_HUMAN_APPROVAL_REQUIRED",
							PolicySeverity.BLOCKING,
							"HIGH 이상 위험 Action은 반드시 Human approval이 필요합니다."
					)
			)));
		}

		return Mono.just(PolicyEvaluationResult.allow());
	}
}
