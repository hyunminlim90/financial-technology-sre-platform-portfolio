package com.fintech.sre.agent.recommendation.approval.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;

class InMemoryRecommendationApprovalAuditLoggerTest {

	@Test
	void shouldStoreAuditLogs() {
		InMemoryRecommendationApprovalAuditLogger logger =
				new InMemoryRecommendationApprovalAuditLogger();

		RecommendationApprovalAuditLog log =
				new RecommendationApprovalAuditLog(
						"audit-1",
						"rec-1",
						"incident-1",
						RecommendationApprovalStatus.APPROVED,
						"operator-a",
						"approved",
						Instant.now(),
						Map.of()
				);

		logger.log(log).block();

		assertThat(
				logger.findByRecommendationRecordId("rec-1")
						.collectList()
						.block()
		).hasSize(1);
	}
}
