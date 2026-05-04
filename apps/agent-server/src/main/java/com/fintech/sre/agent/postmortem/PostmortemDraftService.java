package com.fintech.sre.agent.postmortem;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.actionlog.ActionLog;
import com.fintech.sre.agent.actionlog.ActionLogRepository;
import com.fintech.sre.agent.incident.IncidentLifecycleService;
import com.fintech.sre.agent.incident.IncidentStatus;

import reactor.core.publisher.Mono;

@Service
public class PostmortemDraftService {

	private final ActionLogRepository actionLogRepository;
	private final PostmortemDraftMarkdownRenderer markdownRenderer;
	private final IncidentLifecycleService incidentLifecycleService;

	public PostmortemDraftService(
			ActionLogRepository actionLogRepository,
			PostmortemDraftMarkdownRenderer markdownRenderer,
			IncidentLifecycleService incidentLifecycleService
	) {
		this.actionLogRepository = actionLogRepository;
		this.markdownRenderer = markdownRenderer;
		this.incidentLifecycleService = incidentLifecycleService;
	}

	public Mono<PostmortemDraftInput> prepareDraftInput(String incidentId) {
		return actionLogRepository.findByIncidentId(incidentId)
				.collectList()
				.map(logs -> new PostmortemDraftInput(incidentId, logs));
	}

	public Mono<PostmortemDraftResponse> generateDraft(String incidentId) {
		return prepareDraftInput(incidentId)
				.map(input -> {
					PostmortemDraft draft = new PostmortemDraft(
							incidentId,
							"Incident " + incidentId,
							"확인 필요",
							"확인 필요",
							input.actionLogs(),
							extractLearningCandidates(input),
							Instant.now(),
							true
					);

					return new PostmortemDraftResponse(
							draft.incidentId(),
							draft.title(),
							draft.rootCause(),
							draft.impactSummary(),
							draft.learningCandidates(),
							markdownRenderer.render(draft),
							draft.createdAt(),
							draft.requiresHumanReview()
					);
				})
				.flatMap(response -> incidentLifecycleService.advanceTo(
								incidentId,
								IncidentStatus.POSTMORTEM_DRAFT_READY,
								"postmortem draft generated"
						)
						.thenReturn(response));
	}

	private List<String> extractLearningCandidates(PostmortemDraftInput input) {
		return input.actionLogs().stream()
				.filter(ActionLog::postmortemRequired)
				.map(log -> {
					if (log.command() == null) {
						return "ActionCommand 누락 여부 검토 필요: " + log.id();
					}

					return "ActionCommand `%s` outcome `%s` 기반 runbook/improvement/preventive-design 반영 검토 필요"
							.formatted(log.command().type(), log.outcomeStatus());
				})
				.distinct()
				.toList();
	}
}
