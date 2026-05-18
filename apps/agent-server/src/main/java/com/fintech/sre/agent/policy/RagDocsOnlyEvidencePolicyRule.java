package com.fintech.sre.agent.policy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.evidence.EvidenceContext;

import reactor.core.publisher.Mono;

@Component
public class RagDocsOnlyEvidencePolicyRule implements PolicyRule {

	@Override
	public Mono<PolicyEvaluationResult> evaluate(
			ActionCommand command,
			EvidenceContext evidence
	) {
		if (evidence != null && evidence.hasOnlyRagDocsEvidence()) {
			return Mono.just(PolicyEvaluationResult.deny(List.of(
					new PolicyViolation(
							"POLICY_RAG_DOCS_ONLY_ACTION_BLOCKED",
							PolicySeverity.BLOCKING,
							"rag/docs 단독 근거로는 Action을 추천할 수 없습니다."
					)
			)));
		}

		return Mono.just(PolicyEvaluationResult.allow());
	}
}
