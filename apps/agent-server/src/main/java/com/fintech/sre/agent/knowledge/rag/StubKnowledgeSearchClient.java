package com.fintech.sre.agent.knowledge.rag;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
		name = "agent.knowledge.search.client",
		havingValue = "stub",
		matchIfMissing = true
)
public class StubKnowledgeSearchClient implements KnowledgeSearchClient {

	@Override
	public Mono<KnowledgeSearchResult> search(KnowledgeSearchRequest request) {
		return Mono.just(new KnowledgeSearchResult(List.of(
				new KnowledgeDocument(
						"scenario-payment-high-latency",
						KnowledgeLayer.SCENARIO,
						"scenarios/payment-api/high-latency.md",
						"Payment API High Latency",
						"결제 API latency spike scenario",
						Map.of("domain", "payment")
				),
				new KnowledgeDocument(
						"runbook-payment-high-latency",
						KnowledgeLayer.RUNBOOK,
						"runbooks/payment-api/high-latency.md",
						"Payment API High Latency Runbook",
						"rate limit, scale out, verification, rollback",
						Map.of("domain", "payment")
				),
				new KnowledgeDocument(
						"rag-doc-latency",
						KnowledgeLayer.RAG_DOC,
						"rag/docs/latency.md",
						"Latency Basics",
						"p95 latency, saturation, queueing",
						Map.of("domain", "general")
				)
		)));
	}
}
