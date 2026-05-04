package com.fintech.sre.agent.rag;

import java.util.List;
import java.util.stream.Stream;

public record RagSearchResult(
		List<RagDocument> protocols,
		List<RagDocument> scenarios,
		List<RagDocument> runbooks,
		List<RagDocument> improvements,
		List<RagDocument> preventiveDesigns,
		List<RagDocument> postmortems,
		List<RagDocument> ragDocs
) {

	public boolean hasScenario() {
		return scenarios != null && !scenarios.isEmpty();
	}

	public boolean hasRunbook() {
		return runbooks != null && !runbooks.isEmpty();
	}

	public boolean hasPrimaryKnowledge() {
		return hasScenario() || hasRunbook()
				|| notEmpty(improvements)
				|| notEmpty(preventiveDesigns)
				|| notEmpty(postmortems);
	}

	private boolean notEmpty(List<?> list) {
		return list != null && !list.isEmpty();
	}

	public List<RagDocument> allPrimary() {
		return Stream.of(
				scenarios == null ? List.<RagDocument>of() : scenarios,
				runbooks == null ? List.<RagDocument>of() : runbooks,
				improvements == null ? List.<RagDocument>of() : improvements,
				preventiveDesigns == null ? List.<RagDocument>of() : preventiveDesigns,
				postmortems == null ? List.<RagDocument>of() : postmortems
		).flatMap(List::stream).toList();
	}

	public String summary() {
		return """
				protocols=%s
				scenarios=%s
				runbooks=%s
				improvements=%s
				preventiveDesigns=%s
				postmortems=%s
				ragDocs=%s
				""".formatted(
				titles(protocols),
				titles(scenarios),
				titles(runbooks),
				titles(improvements),
				titles(preventiveDesigns),
				titles(postmortems),
				titles(ragDocs)
		);
	}

	private List<String> titles(List<RagDocument> documents) {
		return documents == null ? List.of() : documents.stream().map(RagDocument::title).toList();
	}
}
