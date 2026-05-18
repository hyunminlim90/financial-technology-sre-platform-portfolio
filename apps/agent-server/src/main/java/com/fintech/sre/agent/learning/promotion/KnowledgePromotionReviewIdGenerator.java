package com.fintech.sre.agent.learning.promotion;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class KnowledgePromotionReviewIdGenerator {

	public String generate() {
		return "knowledge-promotion-review-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
