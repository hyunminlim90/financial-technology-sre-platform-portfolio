package com.fintech.sre.agent.rag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.exception.NoScenarioMatchException;
import com.fintech.sre.agent.model.common.IncidentContext;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RagRetrievalService {

	private final RagVectorStoreClient vectorStoreClient;

	public Mono<RagSearchResult> retrieve(IncidentContext context) {
		RagSearchQuery query = RagSearchQuery.from(context);

		return vectorStoreClient.search(query)
				.map(this::groupByKnowledgeType)
				.flatMap(this::validateMinimumKnowledge);
	}

	private RagSearchResult groupByKnowledgeType(List<RagDocument> documents) {
		Map<KnowledgeType, List<RagDocument>> grouped = documents.stream()
				.collect(Collectors.groupingBy(RagDocument::knowledgeType));

		return new RagSearchResult(
				sort(grouped.getOrDefault(KnowledgeType.PROTOCOL, List.of())),
				sort(grouped.getOrDefault(KnowledgeType.SCENARIO, List.of())),
				sort(grouped.getOrDefault(KnowledgeType.RUNBOOK, List.of())),
				sort(grouped.getOrDefault(KnowledgeType.IMPROVEMENT, List.of())),
				sort(grouped.getOrDefault(KnowledgeType.PREVENTIVE_DESIGN, List.of())),
				sort(grouped.getOrDefault(KnowledgeType.POSTMORTEM, List.of())),
				sort(grouped.getOrDefault(KnowledgeType.RAG_DOC, List.of()))
		);
	}

	private List<RagDocument> sort(List<RagDocument> documents) {
		return documents.stream()
				.sorted((left, right) -> Double.compare(right.score(), left.score()))
				.toList();
	}

	private Mono<RagSearchResult> validateMinimumKnowledge(RagSearchResult result) {
		if (!result.hasScenario()) {
			return Mono.error(new NoScenarioMatchException("No Scenario -> No Action"));
		}

		return Mono.just(result);
	}
}
