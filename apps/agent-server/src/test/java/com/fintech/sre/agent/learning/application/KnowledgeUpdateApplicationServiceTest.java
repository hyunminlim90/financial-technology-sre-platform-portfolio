package com.fintech.sre.agent.learning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;
import com.fintech.sre.agent.learning.plan.InMemoryKnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanTarget;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionTargetType;

class KnowledgeUpdateApplicationServiceTest {

	@Test
	void shouldRecordKnowledgeUpdateApplication() {
		InMemoryKnowledgePromotionPlanStore planStore =
				new InMemoryKnowledgePromotionPlanStore();
		InMemoryKnowledgeUpdateApplicationStore applicationStore =
				new InMemoryKnowledgeUpdateApplicationStore();

		planStore.save(plan()).block();

		KnowledgeUpdateApplicationService service =
				new KnowledgeUpdateApplicationService(
						planStore,
						applicationStore,
						new KnowledgeUpdateApplicationIdGenerator(),
						MetricsRecorderTestSupport.learningMetricsRecorder()
				);

		KnowledgeUpdateApplicationResponse response = service.apply(
				"candidate-1",
				new KnowledgeUpdateApplicationRequest(
						"plan-1",
						"RUNBOOK",
						KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
						"runbooks/payment/payment-api-runbook.md",
						KnowledgeUpdateChangeType.UPDATED,
						"portfolio-repo",
						"main",
						"a1b2c3d4",
						"PR-101",
						"operator-a",
						"reviewer-a",
						"approver-a",
						List.of(
								"Markdown lint passed",
								"customer payload verified"
						),
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		assertThat(response.gitCommitSha())
				.isEqualTo("a1b2c3d4");

		KnowledgeUpdateApplicationRecord record =
				applicationStore.findById(
						response.knowledgeUpdateApplicationId()
				).block();

		assertThat(record.validationChecks())
				.doesNotContain("customer payload verified");
		assertThat(record.metadata())
				.containsKey("team")
				.doesNotContainKey("paymentPayload");
	}

	@Test
	void shouldRejectWhenPromotionPlanMissing() {
		KnowledgeUpdateApplicationService service =
				new KnowledgeUpdateApplicationService(
						new InMemoryKnowledgePromotionPlanStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						new KnowledgeUpdateApplicationIdGenerator(),
						MetricsRecorderTestSupport.learningMetricsRecorder()
				);

		assertThatThrownBy(() -> service.apply(
				"candidate-1",
				new KnowledgeUpdateApplicationRequest(
						"missing",
						"RUNBOOK",
						KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
						"runbooks/payment/payment-api-runbook.md",
						KnowledgeUpdateChangeType.UPDATED,
						"portfolio-repo",
						"main",
						"a1b2c3d4",
						"PR-101",
						"operator-a",
						"reviewer-a",
						"approver-a",
						List.of(),
						Map.of()
				)
		).block())
				.isInstanceOf(KnowledgeUpdateApplicationRejectedException.class)
				.hasMessage("Promotion plan not found.");
	}

	@Test
	void shouldRejectWhenFilePathMissing() {
		KnowledgeUpdateApplicationService service =
				new KnowledgeUpdateApplicationService(
						new InMemoryKnowledgePromotionPlanStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						new KnowledgeUpdateApplicationIdGenerator(),
						MetricsRecorderTestSupport.learningMetricsRecorder()
				);

		assertThatThrownBy(() -> service.apply(
				"candidate-1",
				new KnowledgeUpdateApplicationRequest(
						"plan-1",
						"RUNBOOK",
						KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
						" ",
						KnowledgeUpdateChangeType.UPDATED,
						"portfolio-repo",
						"main",
						"a1b2c3d4",
						"PR-101",
						"operator-a",
						"reviewer-a",
						"approver-a",
						List.of(),
						Map.of()
				)
		).block())
				.isInstanceOf(KnowledgeUpdateApplicationRejectedException.class)
				.hasMessage("filePath is required.");
	}

	@Test
	void shouldRejectWhenGitCommitShaMissing() {
		KnowledgeUpdateApplicationService service =
				new KnowledgeUpdateApplicationService(
						new InMemoryKnowledgePromotionPlanStore(),
						new InMemoryKnowledgeUpdateApplicationStore(),
						new KnowledgeUpdateApplicationIdGenerator(),
						MetricsRecorderTestSupport.learningMetricsRecorder()
				);

		assertThatThrownBy(() -> service.apply(
				"candidate-1",
				new KnowledgeUpdateApplicationRequest(
						"plan-1",
						"RUNBOOK",
						KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
						"runbooks/payment/payment-api-runbook.md",
						KnowledgeUpdateChangeType.UPDATED,
						"portfolio-repo",
						"main",
						" ",
						"PR-101",
						"operator-a",
						"reviewer-a",
						"approver-a",
						List.of(),
						Map.of()
				)
		).block())
				.isInstanceOf(KnowledgeUpdateApplicationRejectedException.class)
				.hasMessage("gitCommitSha is required.");
	}

	private KnowledgePromotionPlanRecord plan() {
		return new KnowledgePromotionPlanRecord(
				"plan-1",
				"candidate-1",
				"incident-1",
				KnowledgePromotionPlanStatus.PLAN_CREATED,
				"operator-a",
				"Plan the manual knowledge update.",
				List.of(new KnowledgePromotionPlanTarget(
						KnowledgePromotionTargetType.RUNBOOK,
						"runbooks/payment/payment-api-runbook.md",
						"Update verification checklist.",
						List.of("Add payment verification section."),
						List.of("Confirm rollback step exists.")
				)),
				List.of("Human must create Git commit/PR outside agent-server."),
				List.of(),
				Instant.now(),
				Map.of(
						"domain", "payment",
						"service", "payment-api"
				)
		);
	}
}
