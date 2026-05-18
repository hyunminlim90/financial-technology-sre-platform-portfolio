package com.fintech.sre.agent.governance.console;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceConsoleHealthController {

	private final GovernanceConsoleHealthService service;

	public GovernanceConsoleHealthController(
			GovernanceConsoleHealthService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/console/health")
	public Mono<GovernanceConsoleHealthResponse> health() {
		return service.health();
	}
}
