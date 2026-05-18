package com.fintech.sre.agent.governance.detail;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceDetailController {

	private final GovernanceIncidentDetailService incidentService;
	private final GovernanceRecommendationDetailService recommendationService;
	private final GovernanceLearningDetailService learningDetailService;
	private final GovernanceKnowledgeUpdateDetailService knowledgeUpdateService;

	public GovernanceDetailController(
			GovernanceIncidentDetailService incidentService,
			GovernanceRecommendationDetailService recommendationService,
			GovernanceLearningDetailService learningDetailService,
			GovernanceKnowledgeUpdateDetailService knowledgeUpdateService
	) {
		this.incidentService = incidentService;
		this.recommendationService = recommendationService;
		this.learningDetailService = learningDetailService;
		this.knowledgeUpdateService = knowledgeUpdateService;
	}

	@GetMapping("/internal/governance/details/incidents/{incidentId}")
	public Mono<GovernanceIncidentDetailResponse> incidentDetail(
			@PathVariable String incidentId
	) {
		return incidentService.findByIncidentId(incidentId);
	}

	@GetMapping("/internal/governance/details/recommendations/{recommendationRecordId}")
	public Mono<GovernanceRecommendationDetailResponse> recommendationDetail(
			@PathVariable String recommendationRecordId
	) {
		return recommendationService.findByRecommendationRecordId(recommendationRecordId);
	}

	@GetMapping("/internal/governance/details/learning-candidates/{learningCandidateId}")
	public Mono<GovernanceLearningDetailResponse> learningDetail(
			@PathVariable String learningCandidateId
	) {
		return learningDetailService.findByLearningCandidateId(learningCandidateId);
	}

	@GetMapping("/internal/governance/details/knowledge-updates/{knowledgeUpdateApplicationId}")
	public Mono<GovernanceKnowledgeUpdateDetailResponse> knowledgeUpdateDetail(
			@PathVariable String knowledgeUpdateApplicationId
	) {
		return knowledgeUpdateService.findByKnowledgeUpdateApplicationId(
				knowledgeUpdateApplicationId
		);
	}
}
