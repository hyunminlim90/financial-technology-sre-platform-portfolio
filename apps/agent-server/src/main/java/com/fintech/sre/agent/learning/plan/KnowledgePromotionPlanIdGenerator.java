package com.fintech.sre.agent.learning.plan;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class KnowledgePromotionPlanIdGenerator {

	public String generate() {
		return "knowledge-promotion-plan-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
