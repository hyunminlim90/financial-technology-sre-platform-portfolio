package com.fintech.sre.agent.knowledge.rag;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class KnowledgeConsumerPolicy {

	public List<KnowledgeConsumerPolicyViolation> validate(KnowledgeContext context) {
		List<KnowledgeConsumerPolicyViolation> violations = new ArrayList<>();

		if (context == null) {
			violations.add(new KnowledgeConsumerPolicyViolation(
					"KNOWLEDGE_CONTEXT_MISSING",
					"KnowledgeContext가 없습니다."
			));
			return violations;
		}

		if (!context.hasScenario()) {
			violations.add(new KnowledgeConsumerPolicyViolation(
					"NO_SCENARIO_NO_ACTION",
					"Scenario 없는 Action 추천은 금지됩니다."
			));
		}

		if (!context.hasRunbook()) {
			violations.add(new KnowledgeConsumerPolicyViolation(
					"RUNBOOK_REQUIRED",
					"Runbook 없는 Action 추천은 금지됩니다."
			));
		}

		if (context.onlyRagDocs()) {
			violations.add(new KnowledgeConsumerPolicyViolation(
					"RAG_DOCS_ONLY_ACTION_FORBIDDEN",
					"rag/docs만으로 Action을 결정할 수 없습니다."
			));
		}

		return violations;
	}
}
