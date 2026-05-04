package com.fintech.sre.agent.knowledge.vector;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.rag.KnowledgeDocument;
import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;
import com.fintech.sre.agent.knowledge.rag.KnowledgeSearchClient;
import com.fintech.sre.agent.knowledge.rag.KnowledgeSearchRequest;
import com.fintech.sre.agent.knowledge.rag.KnowledgeSearchResult;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
		name = "agent.knowledge.search.client",
		havingValue = "vector"
)
public class VectorKnowledgeSearchClient implements KnowledgeSearchClient {

	private final VectorSearchPort vectorSearchPort;

	public VectorKnowledgeSearchClient(VectorSearchPort vectorSearchPort) {
		this.vectorSearchPort = vectorSearchPort;
	}

	@Override
	public Mono<KnowledgeSearchResult> search(KnowledgeSearchRequest request) {
		VectorSearchRequest vectorRequest = new VectorSearchRequest(
				request.query(),
				mergeLayers(request),
				request.filters(),
				request.limit()
		);

		return vectorSearchPort.search(vectorRequest)
				.map(result -> new KnowledgeSearchResult(
						result.documents().stream()
								.map(document -> new KnowledgeDocument(
										document.id(),
										document.layer(),
										document.path(),
										document.title(),
										document.contentSnippet(),
										document.metadata()
								))
								.toList()
				));
	}

	private List<KnowledgeLayer> mergeLayers(KnowledgeSearchRequest request) {
		List<KnowledgeLayer> layers = new ArrayList<>();

		if (request.requiredLayers() != null) {
			layers.addAll(request.requiredLayers());
		}

		if (request.optionalLayers() != null) {
			layers.addAll(request.optionalLayers());
		}

		return layers.stream().distinct().toList();
	}
}
