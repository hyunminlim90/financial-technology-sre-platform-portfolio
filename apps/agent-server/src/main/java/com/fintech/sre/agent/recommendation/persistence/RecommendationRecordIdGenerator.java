package com.fintech.sre.agent.recommendation.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class RecommendationRecordIdGenerator {

	public String generate() {
		return "recommendation-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
