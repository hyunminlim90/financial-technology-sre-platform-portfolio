package com.fintech.sre.agent.operator;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.actionlog.ActionLogResponse;
import com.fintech.sre.agent.actionlog.ActionLogService;
import com.fintech.sre.agent.actionlog.ActionOutcomeRequest;
import com.fintech.sre.agent.decision.report.DecisionReportRepository;
import com.fintech.sre.agent.decision.report.DecisionReportResponse;
import com.fintech.sre.agent.incident.IncidentLifecycleService;
import com.fintech.sre.agent.incident.IncidentStatus;
import com.fintech.sre.agent.improvement.ImprovementCandidateResponse;
import com.fintech.sre.agent.improvement.ImprovementCandidateService;
import com.fintech.sre.agent.knowledge.KnowledgeUpdateReviewResponse;
import com.fintech.sre.agent.knowledge.KnowledgeUpdateReviewService;

import reactor.core.publisher.Mono;

@Service
public class OperatorReviewService {

	private final ActionLogService actionLogService;
	private final DecisionReportRepository decisionReportRepository;
	private final ImprovementCandidateService improvementCandidateService;
	private final KnowledgeUpdateReviewService knowledgeUpdateReviewService;
	private final IncidentLifecycleService incidentLifecycleService;

	public OperatorReviewService(
			ActionLogService actionLogService,
			DecisionReportRepository decisionReportRepository,
			ImprovementCandidateService improvementCandidateService,
			KnowledgeUpdateReviewService knowledgeUpdateReviewService,
			IncidentLifecycleService incidentLifecycleService
	) {
		this.actionLogService = actionLogService;
		this.decisionReportRepository = decisionReportRepository;
		this.improvementCandidateService = improvementCandidateService;
		this.knowledgeUpdateReviewService = knowledgeUpdateReviewService;
		this.incidentLifecycleService = incidentLifecycleService;
	}

	public Mono<OperatorReviewSummary> getIncidentReviewSummary(String incidentId) {
		Mono<java.util.List<ActionLogResponse>> actionLogs = actionLogService.findByIncidentId(incidentId)
				.map(ActionLogResponse::from)
				.collectList();

		Mono<java.util.List<DecisionReportResponse>> decisionReports = decisionReportRepository.findByIncidentId(incidentId)
				.map(DecisionReportResponse::from)
				.collectList();

		Mono<java.util.List<ImprovementCandidateResponse>> improvementCandidates = improvementCandidateService.findByIncidentId(incidentId)
				.map(ImprovementCandidateResponse::from)
				.collectList();

		Mono<java.util.List<KnowledgeUpdateReviewResponse>> knowledgeReviews = knowledgeUpdateReviewService.findByIncidentId(incidentId)
				.map(KnowledgeUpdateReviewResponse::from)
				.collectList();

		return Mono.zip(actionLogs, decisionReports, improvementCandidates, knowledgeReviews)
				.map(tuple -> new OperatorReviewSummary(
						incidentId,
						tuple.getT1(),
						tuple.getT2(),
						tuple.getT3(),
						tuple.getT4()
				));
	}

	public Mono<ActionLogResponse> approveAction(
			String actionLogId,
			OperatorActionDecisionRequest request
	) {
		return actionLogService.approve(
						actionLogId,
						request == null ? null : request.reason()
				)
				.flatMap(log -> incidentLifecycleService.transition(
								log.incidentId(),
								IncidentStatus.ACTION_APPROVED,
								request == null ? null : request.reason()
						)
						.thenReturn(log))
				.map(ActionLogResponse::from);
	}

	public Mono<ActionLogResponse> rejectAction(
			String actionLogId,
			OperatorActionDecisionRequest request
	) {
		return actionLogService.reject(
						actionLogId,
						request == null ? null : request.reason()
				)
				.flatMap(log -> incidentLifecycleService.transition(
								log.incidentId(),
								IncidentStatus.ACTION_REJECTED,
								request == null ? null : request.reason()
						)
						.thenReturn(log))
				.map(ActionLogResponse::from);
	}

	public Mono<ActionLogResponse> reportOutcome(
			String actionLogId,
			OperatorOutcomeReportRequest request
	) {
		return actionLogService.reportOutcome(
						actionLogId,
						new ActionOutcomeRequest(
								request.outcomeStatus(),
								request.outcomeSummary(),
								request.observedSignals()
						)
				)
				.flatMap(log -> incidentLifecycleService.transition(
								log.incidentId(),
								IncidentStatus.OUTCOME_REPORTED,
								request.outcomeSummary()
						)
						.thenReturn(log))
				.map(ActionLogResponse::from);
	}
}
