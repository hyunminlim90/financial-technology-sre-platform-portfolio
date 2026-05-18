package com.fintech.sre.agent.governance.detail;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@Service
public class GovernanceDetailOverviewService {

	private final GovernanceIncidentDetailService incidentDetailService;
	private final GovernanceRecommendationDetailService recommendationDetailService;
	private final GovernanceLearningDetailService learningDetailService;
	private final GovernanceKnowledgeUpdateDetailService knowledgeUpdateDetailService;
	private final GovernanceDetailOverviewBuilder builder;
	private final GovernanceDetailOverviewMetricsRecorder metricsRecorder;

	public GovernanceDetailOverviewService(
			GovernanceIncidentDetailService incidentDetailService,
			GovernanceRecommendationDetailService recommendationDetailService,
			GovernanceLearningDetailService learningDetailService,
			GovernanceKnowledgeUpdateDetailService knowledgeUpdateDetailService,
			GovernanceDetailOverviewBuilder builder,
			GovernanceDetailOverviewMetricsRecorder metricsRecorder
	) {
		this.incidentDetailService = incidentDetailService;
		this.recommendationDetailService = recommendationDetailService;
		this.learningDetailService = learningDetailService;
		this.knowledgeUpdateDetailService = knowledgeUpdateDetailService;
		this.builder = builder;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceDetailOverviewResponse> incidentOverview(
			String incidentId
	) {
		return incidentDetailService.findByIncidentId(incidentId)
				.map(builder::fromIncident)
				.doOnNext(response -> recordSuccess("incident", response))
				.doOnError(ex -> recordOutcomeFailure("incident", ex));
	}

	public Mono<GovernanceDetailOverviewResponse> recommendationOverview(
			String recommendationRecordId
	) {
		return recommendationDetailService.findByRecommendationRecordId(
				recommendationRecordId
		).map(builder::fromRecommendation)
				.doOnNext(response -> recordSuccess("recommendation", response))
				.doOnError(ex -> recordOutcomeFailure("recommendation", ex));
	}

	public Mono<GovernanceDetailOverviewResponse> learningOverview(
			String learningCandidateId
	) {
		return learningDetailService.findByLearningCandidateId(learningCandidateId)
				.map(builder::fromLearning)
				.doOnNext(response -> recordSuccess("learningCandidate", response))
				.doOnError(ex -> recordOutcomeFailure("learningCandidate", ex));
	}

	public Mono<GovernanceDetailOverviewResponse> knowledgeUpdateOverview(
			String knowledgeUpdateApplicationId
	) {
		return knowledgeUpdateDetailService.findByKnowledgeUpdateApplicationId(
				knowledgeUpdateApplicationId
		).map(builder::fromKnowledgeUpdate)
				.doOnNext(response -> recordSuccess("knowledgeUpdate", response))
				.doOnError(ex -> recordOutcomeFailure("knowledgeUpdate", ex));
	}

	private void recordSuccess(
			String detailType,
			GovernanceDetailOverviewResponse response
	) {
		metricsRecorder.success(detailType);
		if (response.degradation() != null && response.degradation().degraded()) {
			metricsRecorder.degraded(detailType, response.degradation().reason());
		}
	}

	private void recordOutcomeFailure(String detailType, Throwable ex) {
		if (isNotFound(ex)) {
			metricsRecorder.notFound(detailType);
			return;
		}
		metricsRecorder.failure(detailType);
	}

	private boolean isNotFound(Throwable ex) {
		return ex instanceof ResponseStatusException status
				&& status.getStatusCode() == HttpStatus.NOT_FOUND;
	}
}
