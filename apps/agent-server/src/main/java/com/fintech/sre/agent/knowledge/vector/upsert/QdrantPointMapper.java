package com.fintech.sre.agent.knowledge.vector.upsert;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.embedding.EmbeddingVector;

@Component
public class QdrantPointMapper {

	public QdrantUpsertRequest.Point toPoint(EmbeddingVector vector) {
		return new QdrantUpsertRequest.Point(
				vector.chunkId(),
				vector.vector(),
				nullSafePayload(vector.payload())
		);
	}

	private Map<String, Object> nullSafePayload(Map<String, Object> payload) {
		return payload == null ? Map.of() : Map.copyOf(payload);
	}
}
