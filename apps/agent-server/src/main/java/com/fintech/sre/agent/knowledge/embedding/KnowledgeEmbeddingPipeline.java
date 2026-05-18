package com.fintech.sre.agent.knowledge.embedding;

import java.nio.file.Path;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class KnowledgeEmbeddingPipeline {

	private final KnowledgeEmbeddingPreparationPipeline preparationPipeline;
	private final EmbeddingService embeddingService;

	public KnowledgeEmbeddingPipeline(
			KnowledgeEmbeddingPreparationPipeline preparationPipeline,
			EmbeddingService embeddingService
	) {
		this.preparationPipeline = preparationPipeline;
		this.embeddingService = embeddingService;
	}

	public Mono<EmbeddingResult> embed(Path rootPath) {
		return preparationPipeline.prepare(rootPath)
				.flatMap(result -> embeddingService.embed(result.requests()));
	}
}
