package com.fintech.sre.agent.admin.knowledge;

public record KnowledgeIngestionAdminRequest(
		String portfolioRootPath,
		String requestedBy,
		String reason,
		boolean dryRun
) {
}
