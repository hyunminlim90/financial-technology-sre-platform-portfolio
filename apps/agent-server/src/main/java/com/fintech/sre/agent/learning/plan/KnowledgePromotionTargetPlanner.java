package com.fintech.sre.agent.learning.plan;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;

@Component
public class KnowledgePromotionTargetPlanner {

	public List<KnowledgePromotionPlanTarget> planTargets(LearningCandidateRecord candidate) {
		if (candidate == null || candidate.type() == null) {
			return List.of();
		}

		KnowledgePromotionTargetType targetType = toTargetType(candidate.type());
		String path = recommendedPath(candidate);
		List<String> proposedChanges = candidate.proposedChanges() == null
				? List.of()
				: candidate.proposedChanges();

		return List.of(new KnowledgePromotionPlanTarget(
				targetType,
				path,
				candidate.summary(),
				proposedChanges,
				validationChecklist(targetType)
		));
	}

	private KnowledgePromotionTargetType toTargetType(LearningCandidateType type) {
		return switch (type) {
			case SCENARIO_UPDATE -> KnowledgePromotionTargetType.SCENARIO;
			case RUNBOOK_UPDATE -> KnowledgePromotionTargetType.RUNBOOK;
			case PREVENTIVE_DESIGN_UPDATE -> KnowledgePromotionTargetType.PREVENTIVE_DESIGN;
			case IMPROVEMENT_UPDATE -> KnowledgePromotionTargetType.IMPROVEMENT;
			case RAG_DOC_UPDATE -> KnowledgePromotionTargetType.RAG_DOC;
			case POLICY_UPDATE -> KnowledgePromotionTargetType.POLICY;
		};
	}

	private String recommendedPath(LearningCandidateRecord candidate) {
		String domain = value(candidate.metadata(), "domain", "general");
		String service = value(candidate.metadata(), "service", "unknown-service");

		return switch (candidate.type()) {
			case SCENARIO_UPDATE -> "scenarios/" + domain + "/" + service + "-scenario.md";
			case RUNBOOK_UPDATE -> "runbooks/" + domain + "/" + service + "-runbook.md";
			case PREVENTIVE_DESIGN_UPDATE -> "preventive-designs/" + domain + "/" + service + "-preventive-design.md";
			case IMPROVEMENT_UPDATE -> "improvements/" + domain + "/" + service + "-improvement.md";
			case RAG_DOC_UPDATE -> "rag/docs/" + domain + "/" + service + "-learning-note.md";
			case POLICY_UPDATE -> "policies/" + domain + "/" + service + "-policy.md";
		};
	}

	private List<String> validationChecklist(KnowledgePromotionTargetType type) {
		return switch (type) {
			case SCENARIO -> List.of(
					"Confirm scenario is tied to a real incident pattern.",
					"Confirm scenario does not allow action without matching evidence.",
					"Confirm No Scenario -> No Action rule remains valid."
			);
			case RUNBOOK -> List.of(
					"Confirm rollback step exists.",
					"Confirm verification step exists.",
					"Confirm payment integrity and duplicate payment checks are included."
			);
			case PREVENTIVE_DESIGN -> List.of(
					"Confirm design prevents recurrence rather than only documenting response.",
					"Confirm blast radius and failure mode are documented.",
					"Confirm operational trade-offs are explicit."
			);
			case IMPROVEMENT -> List.of(
					"Confirm improvement is actionable.",
					"Confirm owner and validation method are clear.",
					"Confirm it does not bypass GitOps or human approval."
			);
			case RAG_DOC -> List.of(
					"Confirm this document is non-actionable reference knowledge.",
					"Confirm it cannot directly trigger ActionCommand.",
					"Confirm source and context are clear."
			);
			case POLICY -> List.of(
					"Confirm policy is enforceable.",
					"Confirm policy does not conflict with existing guardrails.",
					"Confirm FinTech safety constraints are preserved."
			);
		};
	}

	private String value(Map<String, String> metadata, String key, String fallback) {
		if (metadata == null) {
			return fallback;
		}

		String value = metadata.get(key);
		return value == null || value.isBlank() ? fallback : value;
	}
}
