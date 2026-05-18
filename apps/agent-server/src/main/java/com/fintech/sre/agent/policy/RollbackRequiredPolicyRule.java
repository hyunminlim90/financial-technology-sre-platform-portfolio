package com.fintech.sre.agent.policy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.evidence.EvidenceContext;

import reactor.core.publisher.Mono;

@Component
public class RollbackRequiredPolicyRule implements PolicyRule {

	@Override
	public Mono<PolicyEvaluationResult> evaluate(ActionCommand command, EvidenceContext evidence) {
		if (command == null || !command.hasRollback()) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_ROLLBACK_REQUIRED",
							PolicySeverity.BLOCKING,
							"Rollback 없는 ActionCommand는 허용되지 않습니다."
					)
			)));
		}

		return Mono.just(PolicyEvaluationResult.allow());
	}
}
