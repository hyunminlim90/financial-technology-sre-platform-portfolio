package com.fintech.sre.agent.knowledge.embedding;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.knowledge.chunk.KnowledgeChunk;
import com.fintech.sre.agent.knowledge.chunk.KnowledgeChunker;
import com.fintech.sre.agent.knowledge.scanner.KnowledgeScanService;

import reactor.core.publisher.Mono;

@Service
public class KnowledgeEmbeddingPreparationPipeline {

	private final KnowledgeScanService scanService;
	private final KnowledgeChunker chunker;
	private final EmbeddingPreparationService embeddingPreparationService;

	public KnowledgeEmbeddingPreparationPipeline(
			KnowledgeScanService scanService,
			KnowledgeChunker chunker,
			EmbeddingPreparationService embeddingPreparationService
	) {
		this.scanService = scanService;
		this.chunker = chunker;
		this.embeddingPreparationService = embeddingPreparationService;
	}

	public Mono<EmbeddingPreparationResult> prepare(Path rootPath) {
		return scanService.scan(rootPath)
				.map(scanResult -> {
					List<KnowledgeChunk> chunks = scanResult.validDocuments().stream()
							.flatMap(document -> chunker.chunk(document).stream())
							.toList();

					return embeddingPreparationService.prepare(chunks);
				});
	}
}
