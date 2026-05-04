package com.fintech.sre.agent.policy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.evidence.EvidenceContext;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PolicyEngine {

	private final List<PolicyRule> rules;

	public PolicyEngine(List<PolicyRule> rules) {
		this.rules = rules;
	}

	public Mono<PolicyEvaluationResult> evaluate(
			ActionCommand command,
			EvidenceContext evidence
	) {
		return Flux.fromIterable(rules)
				.flatMap(rule -> rule.evaluate(command, evidence))
				.flatMapIterable(PolicyEvaluationResult::violations)
				.collectList()
				.map(violations -> {
					boolean hasBlocking = violations.stream()
							.anyMatch(violation -> violation.severity() == PolicySeverity.BLOCKING);

					if (hasBlocking) {
						return PolicyEvaluationResult.deny(violations);
					}

					return new PolicyEvaluationResult(true, violations);
				});
	}
}
