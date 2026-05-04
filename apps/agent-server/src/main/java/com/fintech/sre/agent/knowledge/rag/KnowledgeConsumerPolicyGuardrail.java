package com.fintech.sre.agent.knowledge.rag;

import java.util.List;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class KnowledgeConsumerPolicyGuardrail {

	private final KnowledgeConsumerPolicy policy;

	public KnowledgeConsumerPolicyGuardrail(KnowledgeConsumerPolicy policy) {
		this.policy = policy;
	}

	public Mono<KnowledgeContext> validate(KnowledgeContext context) {
		List<KnowledgeConsumerPolicyViolation> violations = policy.validate(context);

		if (!violations.isEmpty()) {
			return Mono.error(new KnowledgeConsumerPolicyException(violations));
		}

		return Mono.just(context);
	}
}
