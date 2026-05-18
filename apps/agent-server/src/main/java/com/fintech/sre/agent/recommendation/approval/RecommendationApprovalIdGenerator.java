package com.fintech.sre.agent.recommendation.approval;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class RecommendationApprovalIdGenerator {

	public String generate() {
		return "recommendation-approval-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
