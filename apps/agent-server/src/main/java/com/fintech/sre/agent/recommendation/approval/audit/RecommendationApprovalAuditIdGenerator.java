package com.fintech.sre.agent.recommendation.approval.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class RecommendationApprovalAuditIdGenerator {

	public String generate() {
		return "recommendation-approval-audit-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
