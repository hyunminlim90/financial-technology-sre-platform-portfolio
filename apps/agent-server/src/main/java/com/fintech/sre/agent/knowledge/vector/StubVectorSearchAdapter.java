package com.fintech.sre.agent.knowledge.vector;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
		name = "agent.knowledge.vector.adapter",
		havingValue = "stub",
		matchIfMissing = true
)
public class StubVectorSearchAdapter implements VectorSearchPort {

	@Override
	public Mono<VectorSearchResult> search(VectorSearchRequest request) {
		return Mono.just(new VectorSearchResult(List.of(
				new VectorSearchDocument(
						"scenario-payment-high-latency",
						KnowledgeLayer.SCENARIO,
						"scenarios/payment-api/high-latency.md",
						"Payment API High Latency Scenario",
						"Payment latency spike scenario from portfolio knowledge layer.",
						0.95,
						Map.of("domain", "payment")
				),
				new VectorSearchDocument(
						"runbook-payment-high-latency",
						KnowledgeLayer.RUNBOOK,
						"runbooks/payment-api/high-latency.md",
						"Payment API High Latency Runbook",
						"Recommended mitigation candidates with rollback and verification.",
						0.92,
						Map.of("domain", "payment")
				),
				new VectorSearchDocument(
						"rag-latency-basics",
						KnowledgeLayer.RAG_DOC,
						"rag/docs/latency-basics.md",
						"Latency Basics",
						"Latency, queueing, saturation and p95 analysis.",
						0.80,
						Map.of("domain", "general")
				)
		)));
	}
}
