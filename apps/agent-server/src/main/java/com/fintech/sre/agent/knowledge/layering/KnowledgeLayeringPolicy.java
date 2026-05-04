package com.fintech.sre.agent.knowledge.layering;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.rag.KnowledgeContext;
import com.fintech.sre.agent.knowledge.rag.KnowledgeDocument;

@Component
public class KnowledgeLayeringPolicy {

	public KnowledgeLayeringValidationResult validate(KnowledgeContext context) {
		List<KnowledgeLayeringIssue> issues = new ArrayList<>();

		if (context == null) {
			issues.add(new KnowledgeLayeringIssue(
					"KNOWLEDGE_CONTEXT_MISSING",
					KnowledgeLayeringIssueSeverity.BLOCKING,
					"KnowledgeContext가 없습니다."
			));
			return KnowledgeLayeringValidationResult.invalid(issues);
		}

		if (!context.hasScenario()) {
			issues.add(new KnowledgeLayeringIssue(
					"SCENARIO_REQUIRED",
					KnowledgeLayeringIssueSeverity.BLOCKING,
					"Scenario 없는 장애 대응 Action 추천은 금지됩니다."
			));
		}

		if (!context.hasRunbook()) {
			issues.add(new KnowledgeLayeringIssue(
					"RUNBOOK_REQUIRED",
					KnowledgeLayeringIssueSeverity.BLOCKING,
					"Runbook 없는 장애 대응 Action 추천은 금지됩니다."
			));
		}

		if (context.onlyRagDocs()) {
			issues.add(new KnowledgeLayeringIssue(
					"RAG_DOCS_ONLY_FORBIDDEN",
					KnowledgeLayeringIssueSeverity.BLOCKING,
					"rag/docs만으로 ActionCommand를 생성할 수 없습니다."
			));
		}

		if (!hasPolicyForPaymentContext(context)) {
			issues.add(new KnowledgeLayeringIssue(
					"PAYMENT_POLICY_MISSING",
					KnowledgeLayeringIssueSeverity.WARNING,
					"Payment 관련 KnowledgeContext에 policy 문서가 없습니다. Human review가 필요합니다."
			));
		}

		if (isEmpty(context.preventiveDesigns())) {
			issues.add(new KnowledgeLayeringIssue(
					"PREVENTIVE_DESIGN_NOT_FOUND",
					KnowledgeLayeringIssueSeverity.INFO,
					"Preventive Design 문서가 검색되지 않았습니다."
			));
		}

		boolean blocking = issues.stream()
				.anyMatch(issue -> issue.severity() == KnowledgeLayeringIssueSeverity.BLOCKING);

		return blocking
				? KnowledgeLayeringValidationResult.invalid(issues)
				: KnowledgeLayeringValidationResult.valid(issues);
	}

	private boolean hasPolicyForPaymentContext(KnowledgeContext context) {
		boolean paymentContext =
				containsPayment(context.scenarios())
						|| containsPayment(context.runbooks())
						|| containsPayment(context.ragDocs())
						|| containsPayment(context.improvements())
						|| containsPayment(context.preventiveDesigns());

		if (!paymentContext) {
			return true;
		}

		return context.policies() != null && !context.policies().isEmpty();
	}

	private boolean containsPayment(List<KnowledgeDocument> documents) {
		if (documents == null) {
			return false;
		}

		return documents.stream().anyMatch(document ->
				contains(document.path(), "payment")
						|| contains(document.title(), "payment")
						|| contains(document.contentSnippet(), "payment")
						|| "payment".equalsIgnoreCase(document.metadata() == null ? null : document.metadata().get("domain"))
		);
	}

	private boolean contains(String value, String keyword) {
		return value != null && value.toLowerCase().contains(keyword.toLowerCase());
	}

	private boolean isEmpty(List<?> values) {
		return values == null || values.isEmpty();
	}
}
