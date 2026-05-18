package com.fintech.sre.agent.learning.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;

class KnowledgePromotionTargetPlannerTest {

	private final KnowledgePromotionTargetPlanner planner = new KnowledgePromotionTargetPlanner();

	@Test
	void shouldPlanRunbookTarget() {
		LearningCandidateRecord candidate = new LearningCandidateRecord(
				"candidate-1",
				"incident-1",
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.REVIEW_REQUIRED,
				"operator-a",
				"Improve runbook checks.",
				List.of("Add verification checklist."),
				Instant.now(),
				Map.of(
						"domain", "payment",
						"service", "payment-api"
				)
		);

		List<KnowledgePromotionPlanTarget> targets = planner.planTargets(candidate);

		assertThat(targets).hasSize(1);
		assertThat(targets.get(0).targetType()).isEqualTo(KnowledgePromotionTargetType.RUNBOOK);
		assertThat(targets.get(0).recommendedPath()).contains("runbooks");
		assertThat(targets.get(0).validationChecklist()).isNotEmpty();
	}
}
