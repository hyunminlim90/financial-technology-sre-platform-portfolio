package com.fintech.sre.agent.operator;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.VerificationCommand;
import com.fintech.sre.agent.actionlog.ActionLogRepository;
import com.fintech.sre.agent.actionlog.ActionLogService;
import com.fintech.sre.agent.actionlog.ActionLogStatus;
import com.fintech.sre.agent.actionlog.ActionOutcomeStatus;
import com.fintech.sre.agent.actionlog.InMemoryActionLogRepository;
import com.fintech.sre.agent.decision.report.InMemoryDecisionReportRepository;
import com.fintech.sre.agent.incident.InMemoryIncidentLifecycleRepository;
import com.fintech.sre.agent.incident.IncidentLifecycleService;
import com.fintech.sre.agent.incident.IncidentStatus;
import com.fintech.sre.agent.improvement.ImprovementCandidateService;
import com.fintech.sre.agent.improvement.InMemoryImprovementCandidateRepository;
import com.fintech.sre.agent.knowledge.InMemoryKnowledgeUpdateReviewRepository;
import com.fintech.sre.agent.knowledge.KnowledgeUpdateReviewService;
import com.fintech.sre.agent.model.common.RecommendedAction;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OperatorReviewServiceTest {

	private final IncidentLifecycleService lifecycleService =
			new IncidentLifecycleService(new InMemoryIncidentLifecycleRepository());
	private final ActionLogRepository actionLogRepository = new InMemoryActionLogRepository();
	private final ActionLogService actionLogService = new ActionLogService(actionLogRepository);
	private final OperatorReviewService service = new OperatorReviewService(
			actionLogService,
			new InMemoryDecisionReportRepository(),
			new ImprovementCandidateService(
					actionLogRepository,
					new InMemoryImprovementCandidateRepository(),
					lifecycleService
			),
			new KnowledgeUpdateReviewService(
					new InMemoryKnowledgeUpdateReviewRepository(),
					new InMemoryImprovementCandidateRepository(),
					lifecycleService
			),
			lifecycleService
	);

	@Test
	void approveShouldOnlyRecordHumanApproval() {
		StepVerifier.create(lifecycleService.advanceTo("inc-1", IncidentStatus.HUMAN_REVIEW_REQUIRED, "review required")
						.then(recordRecommendation())
						.flatMap(log -> service.approveAction(
								log.id(),
								new OperatorActionDecisionRequest("approved by human")
						)))
				.expectNextMatches(response ->
						response.status() == ActionLogStatus.APPROVED_BY_HUMAN
								&& "approved by human".equals(response.humanDecisionReason())
				)
				.verifyComplete();
	}

	@Test
	void outcomeReportShouldMarkPostmortemRequiredForSideEffect() {
		StepVerifier.create(lifecycleService.advanceTo("inc-1", IncidentStatus.ACTION_APPROVED, "approved for outcome reporting")
						.then(recordRecommendation())
						.flatMap(log -> service.reportOutcome(
								log.id(),
								new OperatorOutcomeReportRequest(
										ActionOutcomeStatus.CAUSED_SIDE_EFFECT,
										"unexpected side effect",
										List.of("payment.error.rate increased")
								)
						)))
				.expectNextMatches(response ->
						response.postmortemRequired()
								&& response.status() == ActionLogStatus.POSTMORTEM_REQUIRED
				)
				.verifyComplete();
	}

	private Mono<com.fintech.sre.agent.actionlog.ActionLog> recordRecommendation() {
		ActionCommand command = new ActionCommand(
				"cmd-1",
				ActionType.RATE_LIMIT,
				new ActionTarget("payment", "payment-service", "policy", "rate-limit", "prod"),
				true,
				new RollbackCommand("remove rate limit"),
				List.of(new VerificationCommand("payment.idempotency.error", "stable", "멱등성 확인"))
		);

		return actionLogService.recordRecommendation(
				"inc-1",
				"scenario-1",
				"runbook-1",
				new RecommendedAction(
						1,
						"Apply rate limit",
						command,
						"reduce load",
						"temporary limit",
						"remove rate limit",
						List.of("verify latency"),
						true,
						null
				)
		);
	}
}
