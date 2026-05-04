package com.fintech.sre.agent.rag;

public record RagDocument(
		String documentId,
		String path,
		String chunkId,
		String title,
		KnowledgeType knowledgeType,
		String content,
		DocumentMetadata metadata,
		double score
) {

	public String failureMode() {
		return metadata != null ? metadata.failureMode() : null;
	}

	public String domain() {
		return metadata != null ? metadata.domain() : null;
	}
}
