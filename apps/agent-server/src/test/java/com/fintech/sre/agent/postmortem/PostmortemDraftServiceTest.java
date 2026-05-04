package com.fintech.sre.agent.postmortem;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.VerificationCommand;
import com.fintech.sre.agent.actionlog.ActionLog;
import com.fintech.sre.agent.actionlog.ActionLogRepository;
import com.fintech.sre.agent.actionlog.ActionLogStatus;
import com.fintech.sre.agent.actionlog.ActionOutcomeStatus;
import com.fintech.sre.agent.actionlog.InMemoryActionLogRepository;
import com.fintech.sre.agent.incident.InMemoryIncidentLifecycleRepository;
import com.fintech.sre.agent.incident.IncidentLifecycleService;

class PostmortemDraftServiceTest {

	@Test
	void shouldPrepareDraftInputFromActionLogs() {
		ActionLogRepository repository = new InMemoryActionLogRepository();
		PostmortemDraftService service = new PostmortemDraftService(
				repository,
				new PostmortemDraftMarkdownRenderer(),
				new IncidentLifecycleService(new InMemoryIncidentLifecycleRepository())
		);

		ActionLog log = new ActionLog(
				"log-1",
				"INC-PM-1",
				"redis-timeout",
				"runbooks/redis/timeout",
				"Apply rate limit",
				new ActionCommand(
						"rate-limit-payment",
						ActionType.RATE_LIMIT,
						new ActionTarget("payment", "payment-api", "policy", "rate-limit", "prod"),
						true,
						new RollbackCommand("Remove rate limit"),
						List.of(new VerificationCommand("error.rate", "decreasing", "Error decreases"))
				),
				ActionLogStatus.RECOMMENDED,
				ActionOutcomeStatus.NOT_REPORTED,
				null,
				null,
				List.of(),
				false,
				Instant.now(),
				Instant.now()
		);

		repository.save(log).block();

		PostmortemDraftInput draftInput = service.prepareDraftInput("INC-PM-1").block();

		assertThat(draftInput).isNotNull();
		assertThat(draftInput.incidentId()).isEqualTo("INC-PM-1");
		assertThat(draftInput.actionLogs()).hasSize(1);
		assertThat(draftInput.actionLogs().get(0).recommendedActionText()).isEqualTo("Apply rate limit");
	}

	@Test
	void generatedDraftMustKeepRootCauseAsPendingAndRequireHumanReview() {
		ActionLogRepository repository = new InMemoryActionLogRepository();
		PostmortemDraftService service = new PostmortemDraftService(
				repository,
				new PostmortemDraftMarkdownRenderer(),
				new IncidentLifecycleService(new InMemoryIncidentLifecycleRepository())
		);

		ActionLog log = new ActionLog(
				"log-2",
				"INC-PM-2",
				"redis-timeout",
				"runbooks/redis/timeout",
				"Apply rate limit",
				new ActionCommand(
						"rate-limit-payment",
						ActionType.RATE_LIMIT,
						new ActionTarget("payment", "payment-api", "policy", "rate-limit", "prod"),
						true,
						new RollbackCommand("Remove rate limit"),
						List.of(new VerificationCommand("error.rate", "decreasing", "Error decreases"))
				),
				ActionLogStatus.POSTMORTEM_REQUIRED,
				ActionOutcomeStatus.ROLLED_BACK,
				"Approved by operator",
				"Rolled back after side effects",
				List.of("error_rate_up"),
				true,
				Instant.now(),
				Instant.now()
		);

		repository.save(log).block();

		PostmortemDraftResponse response = service.generateDraft("INC-PM-2").block();

		assertThat(response).isNotNull();
		assertThat(response.rootCause()).isEqualTo("확인 필요");
		assertThat(response.requiresHumanReview()).isTrue();
		assertThat(response.markdown()).contains("Root Cause: **확인 필요**");
		assertThat(response.learningCandidates()).isNotEmpty();
	}
}
