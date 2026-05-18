package com.fintech.sre.agent.knowledge.scanner;

import java.util.List;

public record RejectedKnowledgeDocument(
		RawKnowledgeSource source,
		List<String> errors
) {
}
