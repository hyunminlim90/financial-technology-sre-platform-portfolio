package com.fintech.sre.agent.improvement;

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

class ImprovementCandidateServiceTest {

	@Test
	void shouldGenerateCandidatesFromPostmortemRequiredActionLog() {
		ActionLogRepository actionLogRepository = new InMemoryActionLogRepository();
		ImprovementCandidateRepository repository = new InMemoryImprovementCandidateRepository();
		ImprovementCandidateService service = new ImprovementCandidateService(
				actionLogRepository,
				repository,
				new IncidentLifecycleService(new InMemoryIncidentLifecycleRepository())
		);

		actionLogRepository.save(new ActionLog(
				"log-1",
				"INC-IMPROVE-1",
				"payment-lag",
				"runbooks/payment-lag.md",
				"Scale out payment workers",
				new ActionCommand(
						"scale-out-payment",
						ActionType.SCALE_OUT,
						new ActionTarget("payment", "payment-worker", "k8s-deployment", "payment-worker", "prod"),
						true,
						new RollbackCommand("Scale down"),
						List.of(new VerificationCommand("lag", "decreasing", "Lag decreases"))
				),
				ActionLogStatus.POSTMORTEM_REQUIRED,
				ActionOutcomeStatus.PARTIALLY_MITIGATED,
				"approved",
				"partially helped",
				List.of("kafka_lag_high", "db_pressure"),
				true,
				Instant.now(),
				Instant.now()
		)).block();

		List<ImprovementCandidate> candidates = service.generateFromIncident("INC-IMPROVE-1")
				.collectList()
				.block();

		assertThat(candidates).isNotEmpty();
		assertThat(candidates).extracting(ImprovementCandidate::type)
				.contains(ImprovementCandidateType.RUNBOOK_UPDATE)
				.contains(ImprovementCandidateType.PREVENTIVE_DESIGN_REQUIRED)
				.contains(ImprovementCandidateType.POLICY_REQUIRED)
				.contains(ImprovementCandidateType.RAG_DOC_UPDATE);
		assertThat(candidates).allMatch(candidate -> candidate.status() == ImprovementCandidateStatus.PROPOSED);
	}
}
