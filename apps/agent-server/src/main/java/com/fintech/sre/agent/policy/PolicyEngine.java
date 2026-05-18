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
		this.rules = rules == null ? List.of() : List.copyOf(rules);
	}

	public Mono<PolicyEvaluationResult> evaluate(
			ActionCommand command,
			EvidenceContext evidence
	) {
		return Flux.fromIterable(rules)
				.concatMap(rule -> rule.evaluate(command, evidence))
				.flatMapIterable(PolicyEvaluationResult::violations)
				.collectList()
				.map(violations -> {
					boolean denied = violations.stream()
							.anyMatch(violation -> violation.severity() == PolicySeverity.BLOCKING);

					if (denied) {
						return PolicyEvaluationResult.deny(violations);
					}

					return PolicyEvaluationResult.allow();
				});
	}
}
