package com.fintech.sre.agent.knowledge.scanner;

import java.nio.file.Path;
import java.util.Map;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;

public record RawKnowledgeSource(
		String id,
		Path path,
		String relativePath,
		String content,
		Map<String, Object> frontmatter,
		KnowledgeDocumentType inferredType
) {
}
