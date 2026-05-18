package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanTarget;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionTargetType;

class KnowledgePromotionPlanEntityMapperTest {

	@Test
	void shouldRoundTripKnowledgePromotionPlanAndPreserveTargets() {
		KnowledgePromotionPlanEntityMapper mapper =
				new KnowledgePromotionPlanEntityMapper(new ObjectMapper());

		KnowledgePromotionPlanRecord record = new KnowledgePromotionPlanRecord(
				"promotion-plan-1",
				"candidate-1",
				"incident-1",
				KnowledgePromotionPlanStatus.PLAN_CREATED,
				"planner-a",
				"Human should prepare a runbook update plan.",
				List.of(new KnowledgePromotionPlanTarget(
						KnowledgePromotionTargetType.RUNBOOK,
						"runbooks/payments/payment-api-runbook.md",
						"Update rollback and verification guidance.",
						List.of("Add rollback verification step."),
						List.of("Confirm rollback step exists.")
				)),
				List.of("Human must edit portfolio knowledge files manually."),
				List.of(),
				Instant.parse("2026-05-09T10:00:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"paymentPayload", "must-not-store"
				)
		);

		KnowledgePromotionPlanEntity entity = mapper.toEntity(record);
		KnowledgePromotionPlanRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("paymentPayload");
		assertThat(restored.targets()).hasSize(1);
		assertThat(restored.targets().get(0).targetType())
				.isEqualTo(KnowledgePromotionTargetType.RUNBOOK);
		assertThat(restored.requiredHumanChecks())
				.containsExactly("Human must edit portfolio knowledge files manually.");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("paymentPayload");
	}
}
