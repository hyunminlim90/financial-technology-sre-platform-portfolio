package com.fintech.sre.agent.policy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.evidence.EvidenceContext;

import reactor.core.publisher.Mono;

@Component
public class ScenarioEvidenceRequiredPolicyRule implements PolicyRule {

	@Override
	public Mono<PolicyEvaluationResult> evaluate(
			ActionCommand command,
			EvidenceContext evidence
	) {
		if (evidence == null || !evidence.hasScenarioEvidence()) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_NO_SCENARIO_NO_ACTION",
							PolicySeverity.BLOCKING,
							"Scenario evidence가 없으므로 Action 추천이 차단됩니다. No Scenario → No Action."
					)
			)));
		}

		return Mono.just(PolicyEvaluationResult.allow());
	}
}
