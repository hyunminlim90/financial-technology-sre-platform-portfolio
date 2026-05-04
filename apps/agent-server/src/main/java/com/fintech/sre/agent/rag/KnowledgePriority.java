package com.fintech.sre.agent.rag;

import java.util.Comparator;

public final class KnowledgePriority {

	private KnowledgePriority() {
	}

	public static Comparator<RagDocument> comparator() {
		return Comparator
				.comparingInt((RagDocument doc) -> priority(doc.knowledgeType()))
				.thenComparing(RagDocument::score, Comparator.reverseOrder());
	}

	private static int priority(KnowledgeType type) {
		return switch (type) {
			case PROTOCOL -> 0;
			case PREVENTIVE_DESIGN -> 1;
			case IMPROVEMENT -> 2;
			case POSTMORTEM -> 3;
			case RUNBOOK -> 4;
			case SCENARIO -> 5;
			case RAG_DOC -> 6;
		};
	}
}
