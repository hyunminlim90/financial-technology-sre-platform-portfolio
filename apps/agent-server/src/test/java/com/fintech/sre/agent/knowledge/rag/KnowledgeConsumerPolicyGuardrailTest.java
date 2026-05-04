package com.fintech.sre.agent.knowledge.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

class KnowledgeConsumerPolicyGuardrailTest {

	private final KnowledgeConsumerPolicyGuardrail guardrail =
			new KnowledgeConsumerPolicyGuardrail(new KnowledgeConsumerPolicy());

	@Test
	void shouldRejectWhenOnlyRagDocsExist() {
		KnowledgeContext context = new KnowledgeContext(
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(new KnowledgeDocument(
						"rag-doc-latency",
						KnowledgeLayer.RAG_DOC,
						"rag/docs/latency.md",
						"Latency Basics",
						"p95 latency, saturation, queueing",
						Map.of("domain", "general")
				)),
				List.of()
		);

		StepVerifier.create(guardrail.validate(context))
				.expectErrorSatisfies(error -> {
					assertThat(error).isInstanceOf(KnowledgeConsumerPolicyException.class);
					KnowledgeConsumerPolicyException exception = (KnowledgeConsumerPolicyException) error;
					assertThat(exception.violations())
							.extracting(KnowledgeConsumerPolicyViolation::code)
							.contains("NO_SCENARIO_NO_ACTION", "RUNBOOK_REQUIRED", "RAG_DOCS_ONLY_ACTION_FORBIDDEN");
				})
				.verify();
	}

	@Test
	void shouldAllowWhenScenarioAndRunbookExist() {
		KnowledgeContext context = new KnowledgeContext(
				List.of(new KnowledgeDocument(
						"scenario-payment-high-latency",
						KnowledgeLayer.SCENARIO,
						"scenarios/payment-api/high-latency.md",
						"Payment API High Latency",
						"scenario snippet",
						Map.of("domain", "payment")
				)),
				List.of(new KnowledgeDocument(
						"runbook-payment-high-latency",
						KnowledgeLayer.RUNBOOK,
						"runbooks/payment-api/high-latency.md",
						"Payment API High Latency Runbook",
						"runbook snippet",
						Map.of("domain", "payment")
				)),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(new KnowledgeDocument(
						"rag-doc-latency",
						KnowledgeLayer.RAG_DOC,
						"rag/docs/latency.md",
						"Latency Basics",
						"rag snippet",
						Map.of("domain", "general")
				)),
				List.of()
		);

		StepVerifier.create(guardrail.validate(context))
				.expectNext(context)
				.verifyComplete();
	}
}
