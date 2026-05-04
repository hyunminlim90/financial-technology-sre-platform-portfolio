package com.fintech.sre.agent.knowledge.rag;

import java.util.List;

public class KnowledgeConsumerPolicyException extends RuntimeException {

	private final List<KnowledgeConsumerPolicyViolation> violations;

	public KnowledgeConsumerPolicyException(List<KnowledgeConsumerPolicyViolation> violations) {
		super("Knowledge consumer policy violation");
		this.violations = violations;
	}

	public List<KnowledgeConsumerPolicyViolation> violations() {
		return violations;
	}
}
