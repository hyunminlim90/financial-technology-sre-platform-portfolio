package com.fintech.sre.agent.knowledge.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;
import com.fintech.sre.agent.knowledge.ingestion.KnowledgeIngestionDocument;

@Component
public class MarkdownKnowledgeChunker implements KnowledgeChunker {

	private static final int DEFAULT_MAX_CHARS = 1_500;
	private static final int DEFAULT_OVERLAP_CHARS = 200;

	@Override
	public List<KnowledgeChunk> chunk(KnowledgeIngestionDocument document) {
		if (document == null || document.content() == null || document.content().isBlank()) {
			return List.of();
		}

		List<String> chunks = splitByHeadingThenSize(document.content());

		List<KnowledgeChunk> result = new ArrayList<>();
		for (int i = 0; i < chunks.size(); i++) {
			String chunkContent = chunks.get(i);

			result.add(new KnowledgeChunk(
					document.id() + "#chunk-" + i,
					document.id(),
					document.type(),
					document.title(),
					document.path(),
					document.domain(),
					document.service(),
					i,
					chunkContent,
					document.summary(),
					safe(document.scenarioIds()),
					safe(document.runbookIds()),
					safe(document.postmortemIds()),
					safe(document.improvementIds()),
					safe(document.preventiveDesignIds()),
					safe(document.policyIds()),
					safe(document.evidenceCodes()),
					safeActionTypes(document.type(), document.actionTypes()),
					safe(document.metadata())
			));
		}

		return result;
	}

	private List<String> splitByHeadingThenSize(String content) {
		List<String> sections = splitByMarkdownHeading(content);
		List<String> chunks = new ArrayList<>();

		for (String section : sections) {
			chunks.addAll(splitBySize(section));
		}

		return chunks.stream()
				.filter(chunk -> !chunk.isBlank())
				.toList();
	}

	private List<String> splitByMarkdownHeading(String content) {
		String[] lines = content.split("\\R");
		List<String> sections = new ArrayList<>();

		StringBuilder current = new StringBuilder();

		for (String line : lines) {
			if (line.startsWith("## ") && !current.isEmpty()) {
				sections.add(current.toString().trim());
				current = new StringBuilder();
			}

			current.append(line).append("\n");
		}

		if (!current.isEmpty()) {
			sections.add(current.toString().trim());
		}

		return sections;
	}

	private List<String> splitBySize(String section) {
		if (section.length() <= DEFAULT_MAX_CHARS) {
			return List.of(section);
		}

		List<String> chunks = new ArrayList<>();
		int start = 0;

		while (start < section.length()) {
			int end = Math.min(start + DEFAULT_MAX_CHARS, section.length());
			chunks.add(section.substring(start, end).trim());

			if (end == section.length()) {
				break;
			}

			start = Math.max(0, end - DEFAULT_OVERLAP_CHARS);
		}

		return chunks;
	}

	private <T> List<T> safe(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}

	private Map<String, String> safe(Map<String, String> values) {
		return values == null ? Map.of() : Map.copyOf(values);
	}

	private List<String> safeActionTypes(
			KnowledgeDocumentType type,
			List<String> actionTypes
	) {
		if (type == KnowledgeDocumentType.RAG_DOC) {
			return List.of();
		}

		return safe(actionTypes);
	}
}
