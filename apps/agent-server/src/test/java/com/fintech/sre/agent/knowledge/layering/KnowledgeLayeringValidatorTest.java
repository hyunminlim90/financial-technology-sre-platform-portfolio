package com.fintech.sre.agent.knowledge.layering;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.rag.KnowledgeContext;
import com.fintech.sre.agent.knowledge.rag.KnowledgeDocument;
import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;

import reactor.test.StepVerifier;

class KnowledgeLayeringValidatorTest {

	private final KnowledgeLayeringValidator validator =
			new KnowledgeLayeringValidator(new KnowledgeLayeringPolicy());

	@Test
	void shouldResolvePriorityAndPreserveIssues() {
		KnowledgeContext context = new KnowledgeContext(
				List.of(document("scenario", KnowledgeLayer.SCENARIO)),
				List.of(document("runbook", KnowledgeLayer.RUNBOOK)),
				List.of(document("postmortem", KnowledgeLayer.POSTMORTEM)),
				List.of(document("improvement", KnowledgeLayer.IMPROVEMENT)),
				List.of(),
				List.of(),
				List.of(document("rag", KnowledgeLayer.RAG_DOC)),
				List.of()
		);

		StepVerifier.create(validator.validate(context))
				.assertNext(validated -> {
					assertThat(validated.context()).isEqualTo(context);
					assertThat(validated.priorityDocuments()).extracting(KnowledgeDocument::layer)
							.containsExactly(
									KnowledgeLayer.IMPROVEMENT,
									KnowledgeLayer.POSTMORTEM,
									KnowledgeLayer.RUNBOOK,
									KnowledgeLayer.SCENARIO,
									KnowledgeLayer.RAG_DOC
							);
					assertThat(validated.issues()).extracting(KnowledgeLayeringIssue::code)
							.contains("PAYMENT_POLICY_MISSING", "PREVENTIVE_DESIGN_NOT_FOUND");
				})
				.verifyComplete();
	}

	private KnowledgeDocument document(String id, KnowledgeLayer layer) {
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
