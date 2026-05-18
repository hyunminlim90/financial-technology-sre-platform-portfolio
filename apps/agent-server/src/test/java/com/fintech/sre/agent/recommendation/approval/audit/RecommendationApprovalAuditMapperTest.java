package com.fintech.sre.agent.recommendation.approval.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;

class RecommendationApprovalAuditMapperTest {

	@Test
	void shouldFilterSensitiveMetadata() {
		RecommendationApprovalAuditMapper mapper =
				new RecommendationApprovalAuditMapper(
						new RecommendationApprovalAuditIdGenerator()
				);

		RecommendationApprovalRecord record =
				new RecommendationApprovalRecord(
						"approval-1",
						"rec-1",
						"incident-1",
						RecommendationApprovalStatus.APPROVED,
						"operator-a",
						"looks safe",
						Instant.now(),
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				);

		RecommendationApprovalAuditLog log =
				mapper.toAuditLog(record);

		assertThat(log.metadata())
				.containsKey("team")
				.doesNotContainKey("paymentPayload");
	}
}
