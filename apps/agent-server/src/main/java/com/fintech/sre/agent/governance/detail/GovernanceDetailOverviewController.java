package com.fintech.sre.agent.governance.detail;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceDetailOverviewController {

	private final GovernanceDetailOverviewService service;

	public GovernanceDetailOverviewController(
			GovernanceDetailOverviewService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/details/overview/incidents/{incidentId}")
	public Mono<GovernanceDetailOverviewResponse> incidentOverview(
			@PathVariable String incidentId
	) {
		return service.incidentOverview(incidentId);
	}

	@GetMapping("/internal/governance/details/overview/recommendations/{recommendationRecordId}")
	public Mono<GovernanceDetailOverviewResponse> recommendationOverview(
			@PathVariable String recommendationRecordId
	) {
		return service.recommendationOverview(recommendationRecordId);
	}

	@GetMapping("/internal/governance/details/overview/learning-candidates/{learningCandidateId}")
	public Mono<GovernanceDetailOverviewResponse> learningOverview(
			@PathVariable String learningCandidateId
	) {
		return service.learningOverview(learningCandidateId);
	}

	@GetMapping("/internal/governance/details/overview/knowledge-updates/{knowledgeUpdateApplicationId}")
	public Mono<GovernanceDetailOverviewResponse> knowledgeUpdateOverview(
			@PathVariable String knowledgeUpdateApplicationId
	) {
		return service.knowledgeUpdateOverview(knowledgeUpdateApplicationId);
	}
}
