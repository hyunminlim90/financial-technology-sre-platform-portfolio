package com.fintech.sre.agent.governance.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceDashboardHealthController {

	private final GovernanceDashboardHealthService service;

	public GovernanceDashboardHealthController(
			GovernanceDashboardHealthService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/dashboard/health")
	public Mono<GovernanceDashboardHealthResponse> health() {
		return service.health();
	}
}
