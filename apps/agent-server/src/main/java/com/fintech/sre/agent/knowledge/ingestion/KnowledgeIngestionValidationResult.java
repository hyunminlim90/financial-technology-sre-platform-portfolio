package com.fintech.sre.agent.knowledge.ingestion;

import java.util.List;

public record KnowledgeIngestionValidationResult(
		boolean valid,
		List<String> errors,
		List<String> warnings
) {
	public KnowledgeIngestionValidationResult {
		errors = errors == null ? List.of() : List.copyOf(errors);
		warnings = warnings == null ? List.of() : List.copyOf(warnings);
	}

	public static KnowledgeIngestionValidationResult success() {
		return new KnowledgeIngestionValidationResult(true, List.of(), List.of());
	}

	public static KnowledgeIngestionValidationResult invalid(List<String> errors) {
		return new KnowledgeIngestionValidationResult(false, errors == null ? List.of() : List.copyOf(errors), List.of());
	}

	public static KnowledgeIngestionValidationResult of(
			List<String> errors,
			List<String> warnings
	) {
		return new KnowledgeIngestionValidationResult(
				errors == null || errors.isEmpty(),
				errors == null ? List.of() : List.copyOf(errors),
				warnings == null ? List.of() : List.copyOf(warnings)
		);
	}
}
