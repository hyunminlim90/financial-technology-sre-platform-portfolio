package com.fintech.sre.agent.knowledge.layering;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.rag.KnowledgeContext;
import com.fintech.sre.agent.knowledge.rag.KnowledgeDocument;
import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;

class KnowledgeLayeringPolicyTest {

	private final KnowledgeLayeringPolicy policy = new KnowledgeLayeringPolicy();

	@Test
	void missingScenarioShouldBlockRecommendation() {
		KnowledgeLayeringValidationResult result = policy.validate(new KnowledgeContext(
				List.of(),
				List.of(document(KnowledgeLayer.RUNBOOK, "runbook-payment")),
				List.of(),
				List.of(),
				List.of(),
				List.of(document(KnowledgeLayer.POLICY, "policy-payment")),
				List.of(),
				List.of()
		));

		assertThat(result.valid()).isFalse();
		assertThat(result.issues()).extracting(KnowledgeLayeringIssue::code)
				.contains("SCENARIO_REQUIRED");
	}

	@Test
	void missingRunbookShouldBlockRecommendation() {
		KnowledgeLayeringValidationResult result = policy.validate(new KnowledgeContext(
				List.of(document(KnowledgeLayer.SCENARIO, "scenario-payment")),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(document(KnowledgeLayer.POLICY, "policy-payment")),
				List.of(),
				List.of()
		));

		assertThat(result.valid()).isFalse();
		assertThat(result.issues()).extracting(KnowledgeLayeringIssue::code)
				.contains("RUNBOOK_REQUIRED");
	}

	@Test
	void paymentPolicyMissingShouldBeWarningNotBlocking() {
		KnowledgeLayeringValidationResult result = policy.validate(new KnowledgeContext(
				List.of(document(KnowledgeLayer.SCENARIO, "scenario-payment")),
				List.of(document(KnowledgeLayer.RUNBOOK, "runbook-payment")),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		));

		assertThat(result.valid()).isTrue();
		assertThat(result.issues()).extracting(KnowledgeLayeringIssue::code)
				.contains("PAYMENT_POLICY_MISSING", "PREVENTIVE_DESIGN_NOT_FOUND");
		assertThat(result.issues()).filteredOn(issue -> issue.code().equals("PAYMENT_POLICY_MISSING"))
				.extracting(KnowledgeLayeringIssue::severity)
				.containsExactly(KnowledgeLayeringIssueSeverity.WARNING);
	}

	@Test
	void ragDocsOnlyShouldBlockRecommendation() {
		KnowledgeLayeringValidationResult result = policy.validate(new KnowledgeContext(
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(document(KnowledgeLayer.RAG_DOC, "rag-doc-payment")),
				List.of()
		));

		assertThat(result.valid()).isFalse();
		assertThat(result.issues()).extracting(KnowledgeLayeringIssue::code)
				.contains("RAG_DOCS_ONLY_FORBIDDEN");
	}

	private KnowledgeDocument document(KnowledgeLayer layer, String id) {
		return new KnowledgeDocument(
				id,
				layer,
				layer.name().toLowerCase() + "/payment.md",
				"payment knowledge",
				"payment snippet",
				Map.of("domain", "payment")
		);
	}
}
