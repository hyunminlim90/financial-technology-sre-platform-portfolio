package com.fintech.sre.agent.improvement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.actionlog.ActionLog;
import com.fintech.sre.agent.actionlog.ActionLogRepository;
import com.fintech.sre.agent.actionlog.ActionOutcomeStatus;
import com.fintech.sre.agent.incident.IncidentLifecycleService;
import com.fintech.sre.agent.incident.IncidentStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ImprovementCandidateService {

	private final ActionLogRepository actionLogRepository;
	private final ImprovementCandidateRepository repository;
	private final IncidentLifecycleService incidentLifecycleService;

	public ImprovementCandidateService(
			ActionLogRepository actionLogRepository,
			ImprovementCandidateRepository repository,
			IncidentLifecycleService incidentLifecycleService
	) {
		this.actionLogRepository = actionLogRepository;
		this.repository = repository;
		this.incidentLifecycleService = incidentLifecycleService;
	}

	public Flux<ImprovementCandidate> generateFromIncident(String incidentId) {
		return actionLogRepository.findByIncidentId(incidentId)
				.filter(ActionLog::postmortemRequired)
				.flatMapIterable(this::extractCandidates)
				.flatMap(repository::save)
				.collectList()
				.flatMapMany(candidates -> {
					if (candidates.isEmpty()) {
						return Flux.fromIterable(candidates);
					}
					return incidentLifecycleService.advanceTo(
									incidentId,
									IncidentStatus.IMPROVEMENT_CANDIDATES_CREATED,
									"improvement candidates generated"
							)
							.thenMany(Flux.fromIterable(candidates));
				});
	}

	public Flux<ImprovementCandidate> findByIncidentId(String incidentId) {
		return repository.findByIncidentId(incidentId);
	}

	public Flux<ImprovementCandidate> findProposed() {
		return repository.findByStatus(ImprovementCandidateStatus.PROPOSED);
	}

	public Mono<ImprovementCandidate> accept(String candidateId, String reason) {
		return repository.findById(candidateId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("ImprovementCandidate not found: " + candidateId)))
				.flatMap(candidate -> repository.save(candidate.accept(reason)));
	}

	public Mono<ImprovementCandidate> reject(String candidateId, String reason) {
		return repository.findById(candidateId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("ImprovementCandidate not found: " + candidateId)))
				.flatMap(candidate -> repository.save(candidate.reject(reason)));
	}

	public Mono<ImprovementCandidate> markAppliedExternally(String candidateId, String reason) {
		return repository.findById(candidateId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("ImprovementCandidate not found: " + candidateId)))
				.flatMap(candidate -> repository.save(candidate.markAppliedExternally(reason)));
	}

	private List<ImprovementCandidate> extractCandidates(ActionLog log) {
		List<ImprovementCandidate> candidates = new ArrayList<>();

		if (log.outcomeStatus() == ActionOutcomeStatus.NOT_EFFECTIVE
				|| log.outcomeStatus() == ActionOutcomeStatus.PARTIALLY_MITIGATED) {
			candidates.add(newCandidate(
					log,
					ImprovementCandidateType.RUNBOOK_UPDATE,
					"Runbook update candidate",
					"추천 Action의 효과가 충분하지 않았으므로 runbook 보완 검토가 필요합니다.",
					"runbooks/" + safeScenario(log.scenarioId()) + ".md",
					evidence(log)
			));
		}

		if (log.outcomeStatus() == ActionOutcomeStatus.CAUSED_SIDE_EFFECT
				|| log.outcomeStatus() == ActionOutcomeStatus.ROLLED_BACK) {
			candidates.add(newCandidate(
					log,
					ImprovementCandidateType.GUARDRAIL_RULE_REQUIRED,
					"Guardrail rule candidate",
					"추천 Action이 부작용 또는 rollback을 유발했으므로 guardrail 강화 검토가 필요합니다.",
					"improvements/" + safeScenario(log.scenarioId()) + "-guardrail-hardening.md",
					evidence(log)
			));
		}

		if (log.command() != null
				&& log.command().target() != null
				&& "payment".equalsIgnoreCase(log.command().target().domain())) {
			candidates.add(newCandidate(
					log,
					ImprovementCandidateType.POLICY_REQUIRED,
					"Payment policy candidate",
					"결제 도메인 Action 결과가 기록되었으므로 정합성/idempotency/duplicate payment 정책 검토가 필요합니다.",
					"policies/payment/" + safeScenario(log.scenarioId()) + ".md",
					evidence(log)
			));
		}

		if (log.command() != null
				&& log.command().type() == ActionType.SCALE_OUT
				&& log.outcomeStatus() == ActionOutcomeStatus.PARTIALLY_MITIGATED) {
			candidates.add(newCandidate(
					log,
					ImprovementCandidateType.PREVENTIVE_DESIGN_REQUIRED,
					"Preventive design candidate",
					"Scale-out만으로 완화가 부분적이었으므로 구조적 예방 설계 검토가 필요합니다.",
					"preventive-designs/" + safeScenario(log.scenarioId()) + "-capacity-hardening.md",
					evidence(log)
			));
		}

		if (log.observedSignals() != null && !log.observedSignals().isEmpty()) {
			candidates.add(newCandidate(
					log,
					ImprovementCandidateType.RAG_DOC_UPDATE,
					"RAG docs update candidate",
					"관측 신호가 기록되었으므로 rag/docs에 검색 가능한 운영 지식으로 반영 검토가 필요합니다.",
					"rag/docs/" + safeScenario(log.scenarioId()) + "-signals.md",
					evidence(log)
			));
		}

		return candidates;
	}

	private ImprovementCandidate newCandidate(
			ActionLog log,
			ImprovementCandidateType type,
			String title,
			String reason,
			String targetKnowledgePath,
			List<String> evidence
	) {
		Instant now = Instant.now();

		return new ImprovementCandidate(
				UUID.randomUUID().toString(),
				log.incidentId(),
				log.id(),
				type,
				ImprovementCandidateStatus.PROPOSED,
				title,
				reason,
				targetKnowledgePath,
				evidence,
				null,
				now,
				now
		);
	}

	private List<String> evidence(ActionLog log) {
		List<String> evidence = new ArrayList<>();

		evidence.add("ActionLog ID: " + log.id());
		evidence.add("Incident ID: " + log.incidentId());
		evidence.add("Scenario ID: " + safe(log.scenarioId()));
		evidence.add("Outcome: " + log.outcomeStatus());
		evidence.add("Action: " + safe(log.recommendedActionText()));

		if (log.command() != null) {
			evidence.add("Command Type: " + log.command().type());
			if (log.command().target() != null) {
				evidence.add("Target Domain: " + log.command().target().domain());
				evidence.add("Target Service: " + log.command().target().service());
			}
		}

		if (log.observedSignals() != null) {
			evidence.addAll(log.observedSignals());
		}

		return evidence;
	}

	private String safeScenario(String scenarioId) {
		return safe(scenarioId)
				.replace("/", "-")
				.replace(" ", "-")
				.toLowerCase();
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}
}
