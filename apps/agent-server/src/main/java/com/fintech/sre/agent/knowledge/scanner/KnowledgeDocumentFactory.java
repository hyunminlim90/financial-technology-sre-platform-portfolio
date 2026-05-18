package com.fintech.sre.agent.knowledge.scanner;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;
import com.fintech.sre.agent.knowledge.ingestion.KnowledgeIngestionDocument;

@Component
public class KnowledgeDocumentFactory {

	public KnowledgeIngestionDocument create(RawKnowledgeSource source) {
		Map<String, Object> metadata = source.frontmatter() == null ? Map.of() : source.frontmatter();
		return new KnowledgeIngestionDocument(
				string(metadata, "id", source.id()),
				source.inferredType(),
				string(metadata, "title", extractHeading(source.content())),
				source.relativePath(),
				string(metadata, "domain", inferDomain(source.relativePath())),
				string(metadata, "service", inferService(metadata, source.relativePath())),
				stringList(metadata, "scenarioIds"),
				stringList(metadata, "runbookIds"),
				stringList(metadata, "postmortemIds"),
				stringList(metadata, "improvementIds"),
				stringList(metadata, "preventiveDesignIds"),
				stringList(metadata, "policyIds"),
				stringList(metadata, "evidenceCodes"),
				stringList(metadata, "actionTypes"),
				string(metadata, "summary", summarize(source.content())),
				source.content(),
				stringMap(metadata, "metadata")
		);
	}

	private String string(Map<String, Object> metadata, String key, String fallback) {
		Object value = metadata.get(key);
		if (value == null) {
			return fallback;
		}
		String text = String.valueOf(value);
		return text.isBlank() ? fallback : text;
	}

	private List<String> stringList(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value instanceof List<?> list) {
			return list.stream().map(String::valueOf).toList();
		}
		if (value instanceof String text && !text.isBlank()) {
			return List.of(text);
		}
		return List.of();
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> stringMap(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value instanceof Map<?, ?> rawMap) {
			return rawMap.entrySet().stream()
					.collect(java.util.stream.Collectors.toMap(
							entry -> String.valueOf(entry.getKey()),
							entry -> String.valueOf(entry.getValue())
					));
		}
		return Map.of();
	}

	private String extractHeading(String content) {
		if (content == null || content.isBlank()) {
			return null;
		}
		return content.lines()
				.map(String::trim)
				.filter(line -> line.startsWith("# "))
				.map(line -> line.substring(2).trim())
				.findFirst()
				.orElse(null);
	}

	private String summarize(String content) {
		if (content == null || content.isBlank()) {
			return null;
		}
		return content.lines()
				.map(String::trim)
				.filter(line -> !line.isBlank() && !line.startsWith("---") && !line.startsWith("#"))
				.findFirst()
				.orElse(content.length() > 160 ? content.substring(0, 160) : content);
	}

	private String inferDomain(String relativePath) {
		if (relativePath == null) {
			return null;
		}
		String normalized = relativePath.toLowerCase();
		if (normalized.contains("payment")) {
			return "payment";
		}
		return "platform";
	}

	private String inferService(Map<String, Object> metadata, String relativePath) {
		Object service = metadata.get("service");
		if (service != null && !String.valueOf(service).isBlank()) {
			return String.valueOf(service);
		}

		String normalized = relativePath == null ? "" : relativePath.toLowerCase();
		if (normalized.contains("payment")) {
			return "payment-api";
		}
		return "unknown-service";
	}
}
