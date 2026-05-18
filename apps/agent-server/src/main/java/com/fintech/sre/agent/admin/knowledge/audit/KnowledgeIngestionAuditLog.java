package com.fintech.sre.agent.admin.knowledge.audit;

import java.time.Instant;
import java.util.List;

public record KnowledgeIngestionAuditLog(
		String auditId,
		Instant requestedAt,
		String requestedBy,
		String reason,
		String portfolioRootPath,
		boolean dryRun,
		String status,
		int preparedEmbeddingRequests,
		int embeddedVectors,
		int embeddingFailures,
		int upsertedPoints,
		int upsertFailures,
		List<String> rejectedChunkIds,
		List<String> errors
) {
}
