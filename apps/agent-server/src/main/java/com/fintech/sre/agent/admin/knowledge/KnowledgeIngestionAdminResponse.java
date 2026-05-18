package com.fintech.sre.agent.admin.knowledge;

import java.util.List;

public record KnowledgeIngestionAdminResponse(
		String auditId,
		String status,
		boolean dryRun,
		int preparedEmbeddingRequests,
		int embeddedVectors,
		int embeddingFailures,
		int upsertedPoints,
		int upsertFailures,
		List<String> rejectedChunkIds,
		List<String> errors
) {
	public KnowledgeIngestionAdminResponse {
		rejectedChunkIds = rejectedChunkIds == null ? List.of() : List.copyOf(rejectedChunkIds);
		errors = errors == null ? List.of() : List.copyOf(errors);
	}
}
