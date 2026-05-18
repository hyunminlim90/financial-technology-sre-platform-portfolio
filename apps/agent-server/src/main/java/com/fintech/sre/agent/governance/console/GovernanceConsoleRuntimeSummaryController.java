package com.fintech.sre.agent.governance.console;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceConsoleRuntimeSummaryController {

	private final GovernanceConsoleRuntimeSummaryService service;

	public GovernanceConsoleRuntimeSummaryController(
			GovernanceConsoleRuntimeSummaryService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/console/runtime-summary")
	public Mono<GovernanceConsoleRuntimeSummaryResponse> runtimeSummary() {
		return service.summary();
	}
}
