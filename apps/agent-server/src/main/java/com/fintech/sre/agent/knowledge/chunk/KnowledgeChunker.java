package com.fintech.sre.agent.knowledge.chunk;

import java.util.List;

import com.fintech.sre.agent.knowledge.ingestion.KnowledgeIngestionDocument;

public interface KnowledgeChunker {

	List<KnowledgeChunk> chunk(KnowledgeIngestionDocument document);
}
