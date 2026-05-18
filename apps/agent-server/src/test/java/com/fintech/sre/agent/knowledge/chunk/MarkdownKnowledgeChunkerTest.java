package com.fintech.sre.agent.knowledge.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;
import com.fintech.sre.agent.knowledge.ingestion.KnowledgeIngestionDocument;

class MarkdownKnowledgeChunkerTest {

	private final MarkdownKnowledgeChunker chunker = new MarkdownKnowledgeChunker();

	@Test
	void ragDocChunkMustNotPreserveActionTypes() {
		KnowledgeIngestionDocument document = new KnowledgeIngestionDocument(
				"rag/payment-overview",
				KnowledgeDocumentType.RAG_DOC,
				"Payment Overview",
				"rag/docs/payment-overview.md",
				"payment",
				"payment-api",
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of("SCALE_OUT"),
				"summary",
				"## Overview\ncontent",
				Map.of()
		);

		List<KnowledgeChunk> chunks = chunker.chunk(document);

		assertThat(chunks).isNotEmpty();
		assertThat(chunks.get(0).actionTypes()).isEmpty();
	}
}
