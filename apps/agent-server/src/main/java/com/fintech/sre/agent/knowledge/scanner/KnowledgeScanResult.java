package com.fintech.sre.agent.knowledge.scanner;

import java.util.List;

import com.fintech.sre.agent.knowledge.ingestion.KnowledgeIngestionDocument;

public record KnowledgeScanResult(
		List<KnowledgeIngestionDocument> validDocuments,
		List<RejectedKnowledgeDocument> rejectedDocuments
) {
	public KnowledgeScanResult {
		validDocuments = validDocuments == null ? List.of() : List.copyOf(validDocuments);
		rejectedDocuments = rejectedDocuments == null ? List.of() : List.copyOf(rejectedDocuments);
	}
}
