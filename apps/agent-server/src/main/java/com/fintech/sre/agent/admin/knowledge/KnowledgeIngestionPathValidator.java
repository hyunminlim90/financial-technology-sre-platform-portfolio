package com.fintech.sre.agent.admin.knowledge;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class KnowledgeIngestionPathValidator {

	private final KnowledgeAdminProperties properties;

	public KnowledgeIngestionPathValidator(KnowledgeAdminProperties properties) {
		this.properties = properties;
	}

	public Path validate(String portfolioRootPath) {
		if (!properties.enabledOrDefault()) {
			throw new KnowledgeIngestionRejectedException(
					"KNOWLEDGE_ADMIN_DISABLED",
					"Knowledge ingestion admin API is disabled."
			);
		}

		if (portfolioRootPath == null || portfolioRootPath.isBlank()) {
			throw new KnowledgeIngestionRejectedException(
					"PORTFOLIO_ROOT_PATH_REQUIRED",
					"portfolioRootPath is required."
			);
		}

		Path requestedPath = Path.of(portfolioRootPath)
				.toAbsolutePath()
				.normalize();

		List<Path> allowedRoots = properties.allowedRootPathsOrDefault().stream()
				.map(path -> Path.of(path).toAbsolutePath().normalize())
				.toList();

		boolean allowed = allowedRoots.stream()
				.anyMatch(requestedPath::startsWith);

		if (!allowed) {
			throw new KnowledgeIngestionRejectedException(
					"PORTFOLIO_ROOT_PATH_NOT_ALLOWED",
					"portfolioRootPath is outside allowed root paths."
			);
		}

		return requestedPath;
	}
}
