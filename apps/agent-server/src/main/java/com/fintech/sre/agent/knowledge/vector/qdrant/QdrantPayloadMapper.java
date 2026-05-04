package com.fintech.sre.agent.knowledge.vector.qdrant;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;
import com.fintech.sre.agent.knowledge.vector.VectorSearchDocument;

@Component
public class QdrantPayloadMapper {

	public VectorSearchDocument toDocument(QdrantSearchResponse.QdrantPoint point) {
		Map<String, Object> payload = point.payload();

		return new VectorSearchDocument(
				point.id(),
				parseLayer(asString(payload.get("layer"))),
				asString(payload.get("path")),
				asString(payload.get("title")),
				asString(payload.get("contentSnippet")),
				point.score() == null ? 0.0 : point.score(),
				Map.of(
						"domain", asString(payload.get("domain")),
						"source", asString(payload.get("source"))
				)
		);
	}

	private KnowledgeLayer parseLayer(String value) {
		if (value == null || value.isBlank()) {
			return KnowledgeLayer.RAG_DOC;
		}

		return KnowledgeLayer.valueOf(value.trim().toUpperCase());
	}

	private String asString(Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
