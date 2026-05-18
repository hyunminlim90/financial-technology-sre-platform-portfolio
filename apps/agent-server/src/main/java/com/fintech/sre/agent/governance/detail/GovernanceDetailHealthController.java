package com.fintech.sre.agent.governance.detail;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceDetailHealthController {

	private final GovernanceDetailHealthService service;

	public GovernanceDetailHealthController(
			GovernanceDetailHealthService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/details/health")
	public Mono<GovernanceDetailHealthResponse> health() {
		return service.health();
	}
}
