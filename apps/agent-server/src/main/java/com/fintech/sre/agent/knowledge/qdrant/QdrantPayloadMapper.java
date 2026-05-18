package com.fintech.sre.agent.knowledge.qdrant;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.KnowledgeDocument;
import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;

@Component("knowledgeQdrantPayloadMapper")
public class QdrantPayloadMapper {

	public KnowledgeDocument toDocument(
			String pointId,
			Map<String, Object> payload
	) {
		if (payload == null) {
			return null;
		}

		KnowledgeDocumentType type = toType(string(payload, "type"));

		return new KnowledgeDocument(
				firstNonBlank(string(payload, "id"), pointId),
				type,
				string(payload, "title"),
				string(payload, "path"),
				string(payload, "domain"),
				string(payload, "service"),
				stringList(payload, "scenarioIds"),
				stringList(payload, "runbookIds"),
				stringList(payload, "evidenceCodes"),
				stringList(payload, "actionTypes"),
				score(payload),
				stringMap(payload, "metadata"),
				string(payload, "summary")
		);
	}

	private KnowledgeDocumentType toType(String value) {
		if (value == null || value.isBlank()) {
			return KnowledgeDocumentType.RAG_DOC;
		}

		try {
			return KnowledgeDocumentType.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException ex) {
			return KnowledgeDocumentType.RAG_DOC;
		}
	}

	private String string(Map<String, Object> payload, String key) {
		Object value = payload.get(key);
		return value == null ? null : String.valueOf(value);
	}

	@SuppressWarnings("unchecked")
	private List<String> stringList(Map<String, Object> payload, String key) {
		Object value = payload.get(key);

		if (value instanceof List<?> list) {
			return list.stream()
					.map(String::valueOf)
					.toList();
		}

		if (value instanceof String text && !text.isBlank()) {
			return List.of(text);
		}

		return List.of();
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> stringMap(Map<String, Object> payload, String key) {
		Object value = payload.get(key);
		if (value instanceof Map<?, ?> rawMap) {
			return rawMap.entrySet().stream()
					.collect(java.util.stream.Collectors.toMap(
							entry -> String.valueOf(entry.getKey()),
							entry -> String.valueOf(entry.getValue())
					));
		}
		return Map.of();
	}

	private double score(Map<String, Object> payload) {
		Object value = payload.get("score");

		if (value instanceof Number number) {
			return number.doubleValue();
		}

		return 0.5d;
	}

	private String firstNonBlank(String left, String right) {
		if (left != null && !left.isBlank()) {
			return left;
		}
		return right;
	}
}
