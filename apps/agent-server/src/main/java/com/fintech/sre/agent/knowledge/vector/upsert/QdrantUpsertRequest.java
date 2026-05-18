package com.fintech.sre.agent.knowledge.vector.upsert;

import java.util.List;
import java.util.Map;

public record QdrantUpsertRequest(
		List<Point> points
) {
	public record Point(
			String id,
			List<Float> vector,
			Map<String, Object> payload
	) {
	}
}
