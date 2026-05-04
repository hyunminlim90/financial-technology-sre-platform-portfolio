package com.fintech.sre.agent.knowledge.vector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;
import com.fintech.sre.agent.knowledge.rag.KnowledgeSearchRequest;

import reactor.test.StepVerifier;

class VectorKnowledgeSearchClientTest {

	@Test
	void shouldMapVectorDocumentsIntoKnowledgeSearchResult() {
		VectorKnowledgeSearchClient client = new VectorKnowledgeSearchClient(request ->
				reactor.core.publisher.Mono.just(new VectorSearchResult(List.of(
						new VectorSearchDocument(
								"scenario-1",
								KnowledgeLayer.SCENARIO,
								"scenarios/payment/high-latency.md",
								"Scenario",
								"scenario snippet",
								0.99,
								Map.of("domain", "payment")
						),
						new VectorSearchDocument(
								"runbook-1",
								KnowledgeLayer.RUNBOOK,
								"runbooks/payment/high-latency.md",
								"Runbook",
								"runbook snippet",
								0.95,
								Map.of("domain", "payment")
						)
				))));

		KnowledgeSearchRequest request = new KnowledgeSearchRequest(
				"payment latency spike",
				List.of(KnowledgeLayer.SCENARIO, KnowledgeLayer.RUNBOOK),
				List.of(KnowledgeLayer.RAG_DOC, KnowledgeLayer.RUNBOOK),
				Map.of("domain", "payment"),
				5
		);

		StepVerifier.create(client.search(request))
				.assertNext(result -> {
					assertThat(result.documents()).hasSize(2);
					assertThat(result.documents()).extracting(document -> document.layer())
							.containsExactly(KnowledgeLayer.SCENARIO, KnowledgeLayer.RUNBOOK);
				})
				.verifyComplete();
	}
}
