package com.fintech.sre.agent.governance.timeline;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceTimelineHealthController {

	private final GovernanceTimelineHealthService service;

	public GovernanceTimelineHealthController(
			GovernanceTimelineHealthService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/timeline/health")
	public Mono<GovernanceTimelineHealthResponse> health() {
		return service.health();
	}
}
