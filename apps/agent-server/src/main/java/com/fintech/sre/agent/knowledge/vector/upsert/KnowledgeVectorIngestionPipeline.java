package com.fintech.sre.agent.knowledge.vector.upsert;

import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.knowledge.embedding.EmbeddingPreparationResult;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingResult;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingService;
import com.fintech.sre.agent.knowledge.embedding.KnowledgeEmbeddingPreparationPipeline;

import reactor.core.publisher.Mono;

@Service
public class KnowledgeVectorIngestionPipeline {

	private final KnowledgeEmbeddingPreparationPipeline preparationPipeline;
	private final EmbeddingService embeddingService;
	private final VectorStoreUpsertClient upsertClient;

	public KnowledgeVectorIngestionPipeline(
			KnowledgeEmbeddingPreparationPipeline preparationPipeline,
			EmbeddingService embeddingService,
			VectorStoreUpsertClient upsertClient
	) {
		this.preparationPipeline = preparationPipeline;
		this.embeddingService = embeddingService;
		this.upsertClient = upsertClient;
	}

	public Mono<KnowledgeVectorIngestionResult> ingest(Path rootPath) {
		return preparationPipeline.prepare(rootPath)
				.flatMap(preparation ->
						embeddingService.embed(preparation.requests())
								.flatMap(embedding -> upsertVectors(preparation, embedding))
				);
	}

	private Mono<KnowledgeVectorIngestionResult> upsertVectors(
			EmbeddingPreparationResult preparation,
			EmbeddingResult embedding
	) {
		if (embedding == null || !embedding.hasVectors()) {
			return Mono.just(new KnowledgeVectorIngestionResult(
					preparation,
					embedding == null ? EmbeddingResult.empty() : embedding,
					VectorUpsertResult.empty()
			));
		}

		return upsertClient.upsert(VectorUpsertRequest.of(embedding.vectors()))
				.map(upsert -> new KnowledgeVectorIngestionResult(
						preparation,
						embedding,
						upsert
				));
	}
}
