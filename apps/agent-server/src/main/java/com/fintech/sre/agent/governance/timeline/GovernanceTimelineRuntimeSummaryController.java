package com.fintech.sre.agent.governance.timeline;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceTimelineRuntimeSummaryController {

	private final GovernanceTimelineRuntimeSummaryService service;

	public GovernanceTimelineRuntimeSummaryController(
			GovernanceTimelineRuntimeSummaryService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/timeline/runtime-summary")
	public Mono<GovernanceTimelineRuntimeSummaryResponse> runtimeSummary() {
		return service.summary();
	}
}
