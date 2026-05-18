package com.fintech.sre.agent.governance.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceSearchHealthController {

	private final GovernanceSearchHealthService service;

	public GovernanceSearchHealthController(
			GovernanceSearchHealthService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/search/health")
	public Mono<GovernanceSearchHealthResponse> health() {
		return service.health();
	}
}
